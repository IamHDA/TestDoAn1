package com.vn.backend.services.impl;

import com.vn.backend.dto.request.classroom.*;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.response.classroom.*;
import com.vn.backend.dto.response.common.PagingMeta;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.entities.*;
import com.vn.backend.enums.ClassCodeStatus;
import com.vn.backend.enums.ClassMemberStatus;
import com.vn.backend.enums.ClassroomStatus;
import com.vn.backend.enums.RequestType;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.*;
import com.vn.backend.services.ApprovalRequestService;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.ClassroomService;
import com.vn.backend.utils.MessageUtils;
import com.vn.backend.utils.ModelMapperUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.vn.backend.constants.AppConst.*;

@Service
public class ClassroomServiceImpl extends BaseService implements ClassroomService {

    private final AuthService authService;
    private final ClassroomRepository classroomRepository;
    private final ClassroomSettingRepository classroomSettingRepository;
    private final ClassMemberRepository classMemberRepository;
    private final SubjectRepository subjectRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final ApprovalRequestService approvalRequestService;

    public ClassroomServiceImpl(MessageUtils messageUtils, AuthService authService, ClassroomRepository classroomRepository, ClassroomSettingRepository classroomSettingRepository,
                                ClassMemberRepository classMemberRepository, SubjectRepository subjectRepository, ClassScheduleRepository classScheduleRepository, ApprovalRequestService approvalRequestService) {
        super(messageUtils);
        this.authService = authService;
        this.classroomRepository = classroomRepository;
        this.classroomSettingRepository = classroomSettingRepository;
        this.classMemberRepository = classMemberRepository;
        this.subjectRepository = subjectRepository;
        this.classScheduleRepository = classScheduleRepository;
        this.approvalRequestService = approvalRequestService;
    }

    @Override
    @Transactional
    public void createClassroom(ClassroomCreateRequest request) {
        log.info("Start service to create classroom");

        User user = authService.getCurrentUser();

        // create new classroom
        ClassroomCreateRequestDTO dto = request.toDTO();
        dto.setTeacherId(user.getId());
        dto.setClassCode(this.generateUniqueClassCode());
        dto.setClassCodeStatus(ClassCodeStatus.ACTIVE);
        dto.setClassroomStatus(ClassroomStatus.ACTIVE);

        if (!subjectRepository.existsBySubjectIdAndIsDeletedIsFalse(dto.getSubjectId())) {
            throw new AppException(MessageConst.NOT_FOUND, messageUtils.getMessage(MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST);
        }

        Classroom classroom = toClassroomEntity(dto);
        classroomRepository.save(classroom);

        // create new classroom setting default
        ClassroomSetting setting = this.createClassroomSettingDefault(classroom);
        classroomSettingRepository.save(setting);

        // create class schedule
        List<ClassSchedule> classSchedules = new ArrayList<>();
        for (ClassScheduleRequestDTO scheduleRequestDTO : dto.getClassScheduleRequestDTOS()) {
            classSchedules.add(
                    ClassSchedule.builder()
                            .classroomId(classroom.getClassroomId())
                            .dayOfWeek(scheduleRequestDTO.getDayOfWeek())
                            .startTime(scheduleRequestDTO.getStartTime())
                            .endTime(scheduleRequestDTO.getEndTime())
                            .room(scheduleRequestDTO.getRoom())
                            .build()
            );
        }
        classScheduleRepository.saveAll(classSchedules);

        // create request to admin
        approvalRequestService.createRequest(
                RequestType.CLASS_CREATE,
                dto.getRequestDescription(),
                user.getId(),
                List.of(classroom.getClassroomId()));

        log.info("End service create classroom");
    }

    @Override
    public ResponseListData<ClassroomSearchResponse> searchClassroom(BaseFilterSearchRequest<ClassroomSearchRequest> request) {
        log.info("Start service to search classroom");

        User user = authService.getCurrentUser();
        ClassroomSearchRequestDTO requestDTO = request.getFilters().toDTO();
        requestDTO.setUserId(user.getId());
        requestDTO.setClassMemberStatus(ClassMemberStatus.ACTIVE);

        Pageable pageable = request.getPagination().getPagingMeta().toPageable();

        Page<ClassroomSearchQueryDTO> queryDTOS = classroomRepository.searchClassroom(requestDTO, pageable);
        List<ClassroomSearchResponse> responseList = queryDTOS.stream()
                .map(ClassroomSearchResponse::fromDTO)
                .toList();

        PagingMeta pagingMeta = request.getPagination().getPagingMeta();
        pagingMeta.setTotalRows(queryDTOS.getTotalElements());
        pagingMeta.setTotalPages(queryDTOS.getTotalPages());

        log.info("End service search classroom");
        return new ResponseListData<>(responseList, pagingMeta);
    }

    @Override
    public ClassroomDetailResponse getDetailClassroom(String classroomId) {
        log.info("Start service to get detail classroom");

        Classroom classroom = classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(
                Long.parseLong(classroomId), ClassroomStatus.ACTIVE
        ).orElseThrow(() -> new AppException(MessageConst.NOT_FOUND,
                messageUtils.getMessage(MessageConst.NOT_FOUND), HttpStatus.NOT_FOUND));

        log.info("End service to get detail classroom");
        return ClassroomDetailResponse.fromEntity(classroom);
    }

    @Override
    public ClassroomHeaderResponse getClassroomHeader(String classroomId) {
        log.info("Start service to get classroom header");

        Classroom classroom = classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(
                Long.parseLong(classroomId), ClassroomStatus.ACTIVE
        ).orElseThrow(() -> new AppException(MessageConst.NOT_FOUND,
                messageUtils.getMessage(MessageConst.NOT_FOUND), HttpStatus.NOT_FOUND));

        log.info("End service to get classroom header");
        return ClassroomHeaderResponse.fromEntity(classroom);
    }

    @Transactional
    @Override
    public void updateClassroom(String classroomId, ClassroomUpdateRequest request) {
        log.info("Start service to update classroom");

        User user = authService.getCurrentUser();
        ClassroomUpdateRequestDTO requestDTO = request.toDTO();

        Classroom classroom = classroomRepository.findByClassroomIdAndTeacherIdAndIsActiveTrue(
                Long.parseLong(classroomId),
                user.getId()
        ).orElseThrow(() -> new AppException(MessageConst.NOT_FOUND,
                messageUtils.getMessage(MessageConst.NOT_FOUND), HttpStatus.NOT_FOUND));

        this.toUpdateClassroom(requestDTO, classroom);
        if (requestDTO.getClassScheduleRequestDTOS() != null) {
            classroom.getSchedules().clear();
            for (ClassScheduleRequestDTO dto : requestDTO.getClassScheduleRequestDTOS()) {
                ClassSchedule schedule = ClassSchedule.builder()
                        .scheduleId(dto.getId())
                        .classroomId(classroom.getClassroomId())
                        .dayOfWeek(dto.getDayOfWeek())
                        .startTime(dto.getStartTime())
                        .endTime(dto.getEndTime())
                        .room(dto.getRoom())
                        .build();
                schedule.setClassroom(classroom);
                classroom.getSchedules().add(schedule);
            }
        }
        classroomRepository.save(classroom);

        // update schedule
//        Map<Long, ClassSchedule> classSchedules = classroom.getSchedules()
//                .stream()
//                .collect(Collectors.toMap(
//                        ClassSchedule::getScheduleId,
//                        Function.identity()));
//        Map<Long, ClassScheduleRequestDTO> classScheduleRequestDTOMap = requestDTO.getClassScheduleRequestDTOS()
//                .stream()
//                .collect(Collectors.toMap(
//                        ClassScheduleRequestDTO::getId,
//                        Function.identity()
//                ));


        log.info("End service to update classroom");
    }

    @Override
    public void resetClassCode(Long classroomId) {
        log.info("Start service to reset class code");

        User user = authService.getCurrentUser();
        Classroom classroom = classroomRepository.findByClassroomIdAndTeacherIdAndIsActiveTrue(
                classroomId,
                user.getId()
        ).orElseThrow(() -> new AppException(MessageConst.NOT_FOUND,
                messageUtils.getMessage(MessageConst.NOT_FOUND), HttpStatus.NOT_FOUND));

        classroom.setClassCode(this.generateUniqueClassCode());
        classroomRepository.save(classroom);

        log.info("End service to reset class code");
    }

    private Classroom toClassroomEntity(ClassroomCreateRequestDTO dto) {
        return ModelMapperUtils.mapTo(dto, Classroom.class);
    }

    @Override
    public ClassroomSettingDetailResponse getDetailClassroomSetting(String classroomId) {
        log.info("Start service to get detail classroom setting");

        ClassroomSetting classroomSetting = classroomSettingRepository.findByClassroomId(
                Long.parseLong(classroomId)
        ).orElseThrow(() -> new AppException(MessageConst.NOT_FOUND, messageUtils.getMessage(MessageConst.NOT_FOUND), HttpStatus.NOT_FOUND));

        log.info("End service to get detail classroom setting");

        return ClassroomSettingDetailResponse.fromEntity(classroomSetting);
    }

    @Override
    public void updateClassroomSetting(String classroomId, ClassroomSettingUpdateRequest request) {
        log.info("Start service to update classroom setting");

        User user = authService.getCurrentUser();
        ClassroomSettingUpdateRequestDTO requestDTO = request.toDTO();
        ClassroomSetting classroomSetting = classroomSettingRepository.findByClassroomIdAndTeacherId(
                Long.parseLong(classroomId),
                user.getId()
        ).orElseThrow(() -> new AppException(MessageConst.NOT_FOUND, messageUtils.getMessage(MessageConst.NOT_FOUND), HttpStatus.NOT_FOUND));

        this.toUpdateClassroomSetting(requestDTO, classroomSetting);
        classroomSettingRepository.save(classroomSetting);

        log.info("End service to update classroom setting");
    }

    @Override
    public ResponseListData<ClassMemberSearchResponse> searchClassMember(
            BaseFilterSearchRequest<ClassMemberSearchRequest> request) {
        log.info("Start service to search class member");

        User user = authService.getCurrentUser();
        ClassMemberSearchRequestDTO requestDTO = request.getFilters().toDTO();

        if (!this.isMemberOrTeacherOfClass(user, requestDTO.getClassroomId())) {
            throw new AppException(MessageConst.FORBIDDEN, messageUtils.getMessage(MessageConst.FORBIDDEN), HttpStatus.FORBIDDEN);
        }

        Pageable pageable = request.getPagination().getPagingMeta().toPageable();
        Page<ClassMemberSearchQueryDTO> queryDTOS = classMemberRepository.searchClassMember(requestDTO, pageable);
        List<ClassMemberSearchResponse> responseList = queryDTOS.stream()
                .map(ClassMemberSearchResponse::fromDTO)
                .toList();

        PagingMeta pagingMeta = request.getPagination().getPagingMeta();
        pagingMeta.setTotalRows(queryDTOS.getTotalElements());
        pagingMeta.setTotalPages(queryDTOS.getTotalPages());

        log.info("End service to search class member");
        return new ResponseListData<>(responseList, pagingMeta);
    }

    @Override
    public void updateClassMemberStatus(String memberId, ClassMemberStatusUpdateRequest request) {
        log.info("Start service to update class member status");

        User user = authService.getCurrentUser();
        ClassMemberStatusUpdateRequestDTO requestDTO = request.toDTO();

        ClassMember classMember = classMemberRepository.findById(
                Long.parseLong(memberId)
        ).orElseThrow(() -> new AppException(MessageConst.NOT_FOUND, messageUtils.getMessage(MessageConst.NOT_FOUND), HttpStatus.NOT_FOUND));

        if (!classMember.getClassroom().getTeacherId().equals(user.getId())) {
            throw new AppException(MessageConst.FORBIDDEN, messageUtils.getMessage(MessageConst.FORBIDDEN), HttpStatus.NOT_FOUND);
        }
        classMember.setMemberStatus(requestDTO.getClassMemberStatus());
        classMemberRepository.save(classMember);

        log.info("End service to update class member status");
    }

    private ClassroomSetting createClassroomSettingDefault(Classroom classroom) {
        return ClassroomSetting.builder()
                .classroomId(classroom.getClassroomId())
                .allowStudentPost(true)
                .notifyEmail(true)
                .build();
    }

    /**
     * Tạo mã lớp học gồm 6 kí tự ngẫu nhiên và duy nhất trong hệ thống
     *
     * @return mã lớp học tồn tại duy nhất
     */
    private String generateUniqueClassCode() {
        String code;
        int tries = 0;
        do {
            code = generateClassCode();
            tries++;
            if (tries > MAX_TRIES) {
                log.info("Unable to generate unique class code after {} tries", MAX_TRIES);
                throw new AppException(MessageConst.ERR_CLASS_CODE_GENERATION_FAILED,
                        messageUtils.getMessage(MessageConst.ERR_CLASS_CODE_GENERATION_FAILED), HttpStatus.BAD_REQUEST);
            }
        } while (classroomRepository.existsByClassCode(code));
        return code;
    }

    /**
     * Tạo 6 kí tự ngẫu nhiên
     *
     * @return 6 kí tự ngẫu nhiên
     */
    private String generateClassCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = RANDOM.nextInt(CHAR_POOL.length());
            code.append(CHAR_POOL.charAt(index));
        }
        return code.toString();
    }


    private void toUpdateClassroom(ClassroomUpdateRequestDTO dto, Classroom classroom) {
        classroom.setClassName(dto.getClassName());
        classroom.setDescription(dto.getDescription());
        classroom.setCoverImageUrl(dto.getCoverImageUrl());
        classroom.setClassroomStatus(dto.getClassroomStatus());
        classroom.setClassCodeStatus(dto.getClassCodeStatus());
    }

    private void toUpdateClassroomSetting(ClassroomSettingUpdateRequestDTO dto, ClassroomSetting classroomSetting) {
        classroomSetting.setAllowStudentPost(dto.isAllowStudentPost());
    }

    /**
     * kiểm tra user có phải giáo viên hoặc thành viên(ACTIVE) trong lớp không
     *
     * @param user        User
     * @param classroomId classroomId
     * @return isMemberOrTeacherOfClass
     */
    private boolean isMemberOrTeacherOfClass(User user, Long classroomId) {
        return classroomRepository.existsByClassroomIdAndTeacherId(classroomId, user.getId())
                || classMemberRepository.existsByClassroomIdAndUserIdAndMemberStatus(classroomId,
                user.getId(), ClassMemberStatus.ACTIVE);
    }
}
