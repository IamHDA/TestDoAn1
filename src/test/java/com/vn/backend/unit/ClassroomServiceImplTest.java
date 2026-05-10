package com.vn.backend.unit;


import org.junit.jupiter.api.DisplayName;

import com.vn.backend.dto.request.classroom.*;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.common.SearchRequest;
import com.vn.backend.dto.response.classroom.*;
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
import com.vn.backend.services.impl.ClassroomServiceImpl;
import com.vn.backend.utils.MessageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Lớp kiểm thử cho ClassroomServiceImpl, quản lý các unit test cho chức năng
 * lớp học.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClassroomServiceImplTest {

    @Mock
    private AuthService authService;
    @Mock
    private ClassroomRepository classroomRepository;
    @Mock
    private ClassroomSettingRepository classroomSettingRepository;
    @Mock
    private ClassMemberRepository classMemberRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private ClassScheduleRepository classScheduleRepository;
    @Mock
    private ApprovalRequestService approvalRequestService;

    private final AtomicLong classroomIds = new AtomicLong(1);

    private ClassroomServiceImpl service;
    private Classroom savedClassroom;
    private ClassroomSetting savedSetting;
    private List<ClassSchedule> savedSchedules;
    private ClassMember savedClassMember;

    /**
     * Thiết lập môi trường trước mỗi bài kiểm thử.
     */
    @BeforeEach
    void setUp() {
        MessageUtils messageUtils = ServiceTestSupport.mockMessageUtils();

        service = new ClassroomServiceImpl(
                messageUtils,
                authService,
                classroomRepository,
                classroomSettingRepository,
                classMemberRepository,
                subjectRepository,
                classScheduleRepository,
                approvalRequestService);

        when(classroomRepository.save(any(Classroom.class))).thenAnswer(invocation -> {
            savedClassroom = invocation.getArgument(0);
            if (savedClassroom.getClassroomId() == null) {
                savedClassroom.setClassroomId(classroomIds.getAndIncrement());
            }
            return savedClassroom;
        });

        when(classroomSettingRepository.save(any(ClassroomSetting.class))).thenAnswer(invocation -> {
            savedSetting = invocation.getArgument(0);
            return savedSetting;
        });

        when(classScheduleRepository.saveAll(any())).thenAnswer(invocation -> {
            savedSchedules = new ArrayList<>(invocation.getArgument(0));
            return savedSchedules;
        });

        when(classMemberRepository.save(any(ClassMember.class))).thenAnswer(invocation -> {
            savedClassMember = invocation.getArgument(0);
            return savedClassMember;
        });
    }

    private User mockCurrentUser(Long userId) {
        User user = User.builder().id(userId).build();
        when(authService.getCurrentUser()).thenReturn(user);
        return user;
    }

    private ClassroomCreateRequest createRequest() {
        ClassroomCreateRequest request = new ClassroomCreateRequest();
        request.setClassName("SE 101");
        request.setSubjectId("2");
        request.setDescription("Description");
        request.setCoverImageUrl("cover.png");
        request.setStartDate(LocalDate.of(2026, 1, 1));
        request.setEndDate(LocalDate.of(2026, 5, 1));
        request.setCourseOutlineUrl("outline.pdf");
        request.setClassSchedules(List.of(schedule(null, "A1")));
        request.setRequestDescription("Need approval");
        return request;
    }

    private ClassroomUpdateRequest updateRequest() {
        ClassroomUpdateRequest request = new ClassroomUpdateRequest();
        request.setClassName("New");
        request.setDescription("New desc");
        request.setCoverImageUrl("new.png");
        request.setClassroomStatus("ACTIVE");
        request.setClassCodeStatus("DISABLED");
        request.setClassSchedules(List.of(schedule(3L, "B2")));
        return request;
    }

    private ClassScheduleRequest schedule(Long id, String room) {
        ClassScheduleRequest request = new ClassScheduleRequest();
        request.setId(id);
        request.setDayOfWeek(DayOfWeek.MONDAY);
        request.setStartTime(LocalTime.of(8, 0));
        request.setEndTime(LocalTime.of(10, 0));
        request.setRoom(room);
        return request;
    }

    private Classroom classroom(Long classroomId, Long teacherId) {
        Subject subject = new Subject();
        subject.setSubjectId(2L);
        subject.setSubjectName("Java Programming");

        return Classroom.builder()
                .classroomId(classroomId)
                .classCode("ABC123")
                .className("SE 101")
                .teacherId(teacherId)
                .teacher(User.builder().id(teacherId).build())
                .subject(subject)
                .description("Description")
                .coverImageUrl("cover.png")
                .classroomStatus(ClassroomStatus.ACTIVE)
                .classCodeStatus(ClassCodeStatus.ACTIVE)
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 5, 1))
                .courseOutlineUrl("outline.pdf")
                .schedules(new ArrayList<>())
                .build();
    }

    private ClassroomSetting setting(Long classroomId) {
        return ClassroomSetting.builder()
                .classroomId(classroomId)
                .allowStudentPost(true)
                .notifyEmail(true)
                .build();
    }

    private ClassMember classMember(Long memberId, Long teacherId, ClassMemberStatus status) {
        Classroom classroom = Classroom.builder()
                .classroomId(10L)
                .teacherId(teacherId)
                .build();

        return ClassMember.builder()
                .memberId(memberId)
                .classroomId(10L)
                .classroom(classroom)
                .memberStatus(status)
                .build();
    }

    private ClassMemberStatusUpdateRequest memberStatusRequest(String status) {
        ClassMemberStatusUpdateRequest request = new ClassMemberStatusUpdateRequest();
        request.setClassMemberStatus(status);
        return request;
    }

    /**
     * Các bài kiểm thử cho chức năng lấy thông tin lớp học.
     */
    @Nested
    class GetClassroomInformationTests {

        @Test
        @DisplayName("LH_LOP_01 - Đảm bảo xem chi tiết lớp học hoạt động đúng với dữ liệu mock hợp lệ và trả/lưu kết quả theo kỳ vọng.")
        void getDetailClassroom_Success() {
            // Given: Lớp học ID 4 đang ở trạng thái ACTIVE
            when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(4L, ClassroomStatus.ACTIVE))
                    .thenReturn(Optional.of(classroom(4L, 4L)));

            // When: Lấy thông tin chi tiết của lớp học
            ClassroomDetailResponse result = service.getDetailClassroom("4");

            // Then: Kết quả trả về phải chứa thông tin chính xác của lớp học đó
            assertNotNull(result);
            assertEquals(4L, result.getClassroomId());
        }

        @Test
        @DisplayName("LH_LOP_02 - Đảm bảo xem chi tiết lớp học xử lý đúng trường hợp lỗi: fail_throws when classroom missing.")
        void getDetailClassroom_Fail_ThrowsWhenClassroomMissing() {
            when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(4L, ClassroomStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            assertThrows(AppException.class, () -> service.getDetailClassroom("4"));
        }

        @Test
        @DisplayName("LH_LOP_03 - Đảm bảo xem thông tin header lớp học hoạt động đúng với dữ liệu mock hợp lệ và trả/lưu kết quả theo kỳ vọng.")
        void getClassroomHeader_Success() {
            when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(4L, ClassroomStatus.ACTIVE))
                    .thenReturn(Optional.of(classroom(4L, 4L)));

            ClassroomHeaderResponse result = service.getClassroomHeader("4");

            assertNotNull(result);
            assertEquals(4L, result.getClassroomId());
        }

        @Test
        @DisplayName("LH_LOP_04 - Đảm bảo xem thông tin header lớp học xử lý đúng trường hợp lỗi: fail_throws when classroom missing.")
        void getClassroomHeader_Fail_ThrowsWhenClassroomMissing() {
            when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(4L, ClassroomStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            assertThrows(AppException.class, () -> service.getClassroomHeader("4"));
        }
    }

    /**
     * Các bài kiểm thử cho chức năng tìm kiếm lớp học.
     */
    @Nested
    class SearchClassroomTests {

        @Test
        @DisplayName("LH_LOP_05 - Đảm bảo tìm kiếm lớp học hoạt động đúng với dữ liệu mock hợp lệ và trả/lưu kết quả theo kỳ vọng.")
        void searchClassroom_Success() {
            // Given: Danh sách lớp học giả lập kết quả tìm kiếm
            Subject subject1 = new Subject();
            subject1.setSubjectId(1L);
            subject1.setSubjectName("Java Programming");

            Subject subject2 = new Subject();
            subject2.setSubjectId(2L);
            subject2.setSubjectName("Database");

            List<ClassroomSearchQueryDTO> classrooms = List.of(
                    ClassroomSearchQueryDTO.builder()
                            .classroomId(1L)
                            .className("Lap trinh Java co ban")
                            .subject(subject1)
                            .classroomStatus(ClassroomStatus.ACTIVE)
                            .teacherId(10L)
                            .userName("teacher01")
                            .fullName("Nguyen Van A")
                            .memberCount(35L)
                            .assignmentCount(5L)
                            .build(),
                    ClassroomSearchQueryDTO.builder()
                            .classroomId(2L)
                            .className("Co so du lieu")
                            .subject(subject2)
                            .classroomStatus(ClassroomStatus.ACTIVE)
                            .teacherId(11L)
                            .userName("teacher02")
                            .fullName("Tran Thi B")
                            .memberCount(42L)
                            .assignmentCount(8L)
                            .build());

            mockCurrentUser(4L);

            BaseFilterSearchRequest<ClassroomSearchRequest> request = mock(BaseFilterSearchRequest.class);
            ClassroomSearchRequest filters = mock(ClassroomSearchRequest.class);
            ClassroomSearchRequestDTO requestDTO = ClassroomSearchRequestDTO.builder().build();

            SearchRequest pagination = new SearchRequest();
            pagination.setPageNum("1");
            pagination.setPageSize("10");

            when(request.getFilters()).thenReturn(filters);
            when(filters.toDTO()).thenReturn(requestDTO);
            when(request.getPagination()).thenReturn(pagination);

            // Giả lập kết quả trả về từ repository
            when(classroomRepository.searchClassroom(any(ClassroomSearchRequestDTO.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(
                            classrooms,
                            pagination.getPagingMeta().toPageable(),
                            classrooms.size()));

            // When: Thực hiện tìm kiếm lớp học
            ResponseListData<ClassroomSearchResponse> result = service.searchClassroom(request);

            // Then: Kiểm tra dữ liệu tìm kiếm trả về đúng và đầy đủ
            assertNotNull(result);
            assertNotNull(result.getContent());
            assertEquals(classrooms.size(), result.getContent().stream().count());
            assertEquals(classrooms.get(0).getClassroomId(), result.getContent().iterator().next().getClassroomId());
            // Đảm bảo thông tin người dùng thực hiện tìm kiếm được đính kèm vào yêu cầu
            assertEquals(4L, requestDTO.getUserId());
            assertEquals(ClassMemberStatus.ACTIVE, requestDTO.getClassMemberStatus());

            verify(classroomRepository).searchClassroom(any(ClassroomSearchRequestDTO.class), any(Pageable.class));
        }
    }

    /**
     * Các bài kiểm thử cho chức năng tạo lớp học.
     */
    @Nested
    class CreateClassroomTests {

        @Test
        @DisplayName("LH_LOP_06 - Đảm bảo tạo/yêu cầu tạo lớp học hoạt động đúng với dữ liệu mock hợp lệ và trả/lưu kết quả theo kỳ vọng.")
        void createClassroom_Success() {
            // Given: Người dùng có ID 4, môn học tồn tại (ID 2), mã lớp học chưa bị trùng
            mockCurrentUser(4L);
            when(subjectRepository.existsBySubjectIdAndIsDeletedIsFalse(2L)).thenReturn(true);
            when(classroomRepository.existsByClassCode(any())).thenReturn(false);

            // When: Thực hiện tạo lớp học mới
            service.createClassroom(createRequest());

            // Then: Kiểm tra lớp học được lưu với thông tin chính xác (Giáo viên, Trạng
            // thái)
            assertNotNull(savedClassroom);
            assertEquals(4L, savedClassroom.getTeacherId());
            assertEquals(ClassroomStatus.ACTIVE, savedClassroom.getClassroomStatus());
            assertEquals(ClassCodeStatus.ACTIVE, savedClassroom.getClassCodeStatus());

            // Kiểm tra cấu hình lớp học (Setting) được khởi tạo mặc định
            assertNotNull(savedSetting);
            assertEquals(1L, savedSetting.getClassroomId());
            assertTrue(savedSetting.getAllowStudentPost());

            // Kiểm tra lịch học (Schedule) được lưu đúng
            assertNotNull(savedSchedules);
            assertEquals(1, savedSchedules.size());
            assertEquals(1L, savedSchedules.get(0).getClassroomId());

            // Đảm bảo có yêu cầu phê duyệt được tạo ra
            verify(approvalRequestService).createRequest(
                    eq(RequestType.CLASS_CREATE),
                    eq("Need approval"),
                    eq(4L),
                    eq(List.of(1L)));
        }

        @Test
        @DisplayName("LH_LOP_07 - Đảm bảo tạo/yêu cầu tạo lớp học xử lý đúng trường hợp lỗi: fail_throws when subject missing.")
        void createClassroom_Fail_ThrowsWhenSubjectMissing() {
            mockCurrentUser(4L);
            when(subjectRepository.existsBySubjectIdAndIsDeletedIsFalse(2L)).thenReturn(false);
            when(classroomRepository.existsByClassCode(any())).thenReturn(false);

            assertThrows(AppException.class, () -> service.createClassroom(createRequest()));
        }

        @Test
        @DisplayName("LH_LOP_08 - Đảm bảo tạo/yêu cầu tạo lớp học xử lý đúng trường hợp lỗi: fail_throws when request is null.")
        void createClassroom_Fail_ThrowsWhenRequestIsNull() {
            assertThrows(NullPointerException.class, () -> service.createClassroom(null));
        }

        @Test
        @DisplayName("LH_LOP_09 - Đảm bảo tạo/yêu cầu tạo lớp học xử lý đúng trường hợp lỗi: fail_throws when current user is null.")
        void createClassroom_Fail_ThrowsWhenCurrentUserIsNull() {
            when(authService.getCurrentUser()).thenReturn(null);

            assertThrows(NullPointerException.class, () -> service.createClassroom(createRequest()));
        }

        @Test
        @DisplayName("LH_LOP_10 - Đảm bảo tạo/yêu cầu tạo lớp học xử lý đúng trường hợp lỗi: fail_throws when class name exceeds limit.")
        void createClassroom_Fail_ThrowsWhenClassNameExceedsLimit() {
            mockCurrentUser(4L);
            when(subjectRepository.existsBySubjectIdAndIsDeletedIsFalse(2L)).thenReturn(true);

            ClassroomCreateRequest request = createRequest();
            request.setClassName("A".repeat(101));

            assertThrows(AppException.class, () -> service.createClassroom(request));
        }

        @Test
        @DisplayName("LH_LOP_11 - Đảm bảo tạo/yêu cầu tạo lớp học xử lý đúng trường hợp lỗi: fail_throws when description exceeds limit.")
        void createClassroom_Fail_ThrowsWhenDescriptionExceedsLimit() {
            mockCurrentUser(4L);
            when(subjectRepository.existsBySubjectIdAndIsDeletedIsFalse(2L)).thenReturn(true);

            ClassroomCreateRequest request = createRequest();
            request.setDescription("A".repeat(1001));

            assertThrows(AppException.class, () -> service.createClassroom(request));
        }
    }

    /**
     * Các bài kiểm thử cho chức năng cập nhật lớp học.
     */
    @Nested
    class UpdateClassroomTests {

        @Test
        @DisplayName("LH_LOP_12 - Đảm bảo cập nhật thông tin lớp học hoạt động đúng với dữ liệu mock hợp lệ và trả/lưu kết quả theo kỳ vọng.")
        void updateClassroom_Success() {
            // Given: Người dùng (Giáo viên ID 4) đang sửa lớp học ID 9 của chính mình
            mockCurrentUser(4L);

            Classroom classroom = classroom(9L, 4L);
            classroom.setClassName("Old");
            classroom.setDescription("Old desc");
            classroom.setCoverImageUrl("old.png");
            // Lớp học hiện tại đang có lịch học tại phòng A1
            classroom.setSchedules(new ArrayList<>(List.of(
                    ClassSchedule.builder()
                            .scheduleId(1L)
                            .classroomId(9L)
                            .room("A1")
                            .build())));

            when(classroomRepository.findByClassroomIdAndTeacherIdAndIsActiveTrue(9L, 4L))
                    .thenReturn(Optional.of(classroom));

            // When: Thực hiện cập nhật thông tin lớp học (Tên mới, Phòng mới B2, Trạng thái
            // mã lớp DISABLED)
            service.updateClassroom("9", updateRequest());

            // Then: Kiểm tra các thông tin cơ bản đã được cập nhật
            assertNotNull(savedClassroom);
            assertEquals("New", savedClassroom.getClassName());
            assertEquals("New desc", savedClassroom.getDescription());
            assertEquals("new.png", savedClassroom.getCoverImageUrl());
            assertEquals(ClassroomStatus.ACTIVE, savedClassroom.getClassroomStatus());
            assertEquals(ClassCodeStatus.DISABLED, savedClassroom.getClassCodeStatus());

            // Kiểm tra lịch học đã được thay đổi sang phòng B2
            assertEquals(1, savedClassroom.getSchedules().size());
            assertEquals(3L, savedClassroom.getSchedules().get(0).getScheduleId());
            assertEquals(9L, savedClassroom.getSchedules().get(0).getClassroomId());
            assertEquals("B2", savedClassroom.getSchedules().get(0).getRoom());
        }

        @Test
        @DisplayName("LH_LOP_13 - Đảm bảo cập nhật thông tin lớp học xử lý đúng trường hợp lỗi: fail_throws when request is null.")
        void updateClassroom_Fail_ThrowsWhenRequestIsNull() {
            assertThrows(NullPointerException.class, () -> service.updateClassroom("9", null));
        }

        @Test
        @DisplayName("LH_LOP_14 - Đảm bảo cập nhật thông tin lớp học xử lý đúng trường hợp lỗi: fail_throws when current user is null.")
        void updateClassroom_Fail_ThrowsWhenCurrentUserIsNull() {
            when(authService.getCurrentUser()).thenReturn(null);

            assertThrows(NullPointerException.class, () -> service.updateClassroom("9", new ClassroomUpdateRequest()));
        }

        @Test
        @DisplayName("LH_LOP_15 - Đảm bảo cập nhật thông tin lớp học xử lý đúng trường hợp lỗi: fail_throws when classroom missing.")
        void updateClassroom_Fail_ThrowsWhenClassroomMissing() {
            mockCurrentUser(4L);

            when(classroomRepository.findByClassroomIdAndTeacherIdAndIsActiveTrue(9L, 4L))
                    .thenReturn(Optional.empty());

            assertThrows(AppException.class, () -> service.updateClassroom("9", updateRequest()));

            verify(classroomRepository, never()).save(any(Classroom.class));
        }

        @Test
        @DisplayName("LH_LOP_16 - Đảm bảo cập nhật thông tin lớp học xử lý đúng trường hợp lỗi: fail_throws when class name exceeds limit.")
        void updateClassroom_Fail_ThrowsWhenClassNameExceedsLimit() {
            mockCurrentUser(4L);

            ClassroomUpdateRequest request = updateRequest();
            request.setClassName("A".repeat(101));

            assertThrows(AppException.class, () -> service.updateClassroom("9", request));
        }

        @Test
        @DisplayName("LH_LOP_17 - Đảm bảo cập nhật thông tin lớp học xử lý đúng trường hợp lỗi: fail_throws when description exceeds limit.")
        void updateClassroom_Fail_ThrowsWhenDescriptionExceedsLimit() {
            mockCurrentUser(4L);

            ClassroomUpdateRequest request = updateRequest();
            request.setDescription("A".repeat(1001));

            assertThrows(AppException.class, () -> service.updateClassroom("9", request));
        }
    }

    /**
     * Các bài kiểm thử cho chức năng reset mã lớp.
     */
    @Nested
    class ResetClassCodeTests {

        @Test
        @DisplayName("LH_LOP_18 - Đảm bảo cấp lại mã lớp hoạt động đúng với dữ liệu mock hợp lệ và trả/lưu kết quả theo kỳ vọng.")
        void resetClassCode_Success() {
            mockCurrentUser(4L);

            Classroom classroom = classroom(9L, 4L);
            classroom.setClassCode("OLD123");

            when(classroomRepository.findByClassroomIdAndTeacherIdAndIsActiveTrue(9L, 4L))
                    .thenReturn(Optional.of(classroom));
            when(classroomRepository.existsByClassCode(any())).thenReturn(false);

            service.resetClassCode(9L);

            assertNotNull(savedClassroom);
            assertEquals(9L, savedClassroom.getClassroomId());
            assertNotNull(savedClassroom.getClassCode());
            assertEquals(6, savedClassroom.getClassCode().length());
        }

        @Test
        @DisplayName("LH_LOP_19 - Đảm bảo cấp lại mã lớp xử lý đúng trường hợp lỗi: fail_throws when classroom missing.")
        void resetClassCode_Fail_ThrowsWhenClassroomMissing() {
            mockCurrentUser(4L);

            when(classroomRepository.findByClassroomIdAndTeacherIdAndIsActiveTrue(9L, 4L))
                    .thenReturn(Optional.empty());

            assertThrows(AppException.class, () -> service.resetClassCode(9L));

            verify(classroomRepository, never()).save(any(Classroom.class));
        }
    }

    /**
     * Các bài kiểm thử cho chức năng lấy cấu hình lớp học.
     */
    @Nested
    class GetClassroomSettingTests {

        @Test
        @DisplayName("LH_LOP_20 - Đảm bảo xem cài đặt lớp học hoạt động đúng với dữ liệu mock hợp lệ và trả/lưu kết quả theo kỳ vọng.")
        void getDetailClassroomSetting_Success() {
            when(classroomSettingRepository.findByClassroomId(9L))
                    .thenReturn(Optional.of(setting(9L)));

            ClassroomSettingDetailResponse result = service.getDetailClassroomSetting("9");

            assertNotNull(result);
            assertTrue(result.isAllowStudentPost());
        }

        @Test
        @DisplayName("LH_LOP_21 - Đảm bảo xem cài đặt lớp học xử lý đúng trường hợp lỗi: fail_throws when setting missing.")
        void getDetailClassroomSetting_Fail_ThrowsWhenSettingMissing() {
            when(classroomSettingRepository.findByClassroomId(9L))
                    .thenReturn(Optional.empty());

            assertThrows(AppException.class, () -> service.getDetailClassroomSetting("9"));
        }
    }

    /**
     * Các bài kiểm thử cho chức năng cập nhật cấu hình lớp học.
     */
    @Nested
    class UpdateClassroomSettingTests {

        @Test
        @DisplayName("LH_LOP_22 - Đảm bảo cập nhật cài đặt lớp học hoạt động đúng với dữ liệu mock hợp lệ và trả/lưu kết quả theo kỳ vọng.")
        void updateClassroomSetting_Success() {
            mockCurrentUser(4L);

            when(classroomSettingRepository.findByClassroomIdAndTeacherId(9L, 4L))
                    .thenReturn(Optional.of(setting(9L)));

            ClassroomSettingUpdateRequest request = new ClassroomSettingUpdateRequest();
            request.setAllowStudentPost(false);
            request.setNotifyEmail(false);

            service.updateClassroomSetting("9", request);

            assertNotNull(savedSetting);
            assertFalse(savedSetting.getAllowStudentPost());
        }

        @Test
        @DisplayName("LH_LOP_23 - Đảm bảo cập nhật cài đặt lớp học xử lý đúng trường hợp lỗi: fail_throws when current user is not teacher.")
        void updateClassroomSetting_Fail_ThrowsWhenCurrentUserIsNotTeacher() {
            mockCurrentUser(4L);

            when(classroomSettingRepository.findByClassroomIdAndTeacherId(9L, 4L))
                    .thenReturn(Optional.empty());

            assertThrows(AppException.class,
                    () -> service.updateClassroomSetting("9", new ClassroomSettingUpdateRequest()));
        }

        @Test
        @DisplayName("LH_LOP_24 - Đảm bảo cập nhật cài đặt lớp học xử lý đúng trường hợp lỗi: fail_throws when request is null.")
        void updateClassroomSetting_Fail_ThrowsWhenRequestIsNull() {
            assertThrows(NullPointerException.class, () -> service.updateClassroomSetting("9", null));
        }
    }

    /**
     * Các bài kiểm thử cho chức năng cập nhật trạng thái thành viên lớp.
     */
    @Nested
    class UpdateClassMemberStatusTests {

        @Test
        @DisplayName("LH_LOP_25 - Đảm bảo cập nhật trạng thái thành viên hoạt động đúng với dữ liệu mock hợp lệ và trả/lưu kết quả theo kỳ vọng.")
        void updateClassMemberStatus_Success() {
            // Given: Giáo viên đăng nhập và có một thành viên lớp đang ở trạng thái INACTIVE
            mockCurrentUser(4L);

            when(classMemberRepository.findById(9L))
                    .thenReturn(Optional.of(classMember(9L, 4L, ClassMemberStatus.INACTIVE)));

            // When: Cập nhật trạng thái thành viên sang ACTIVE
            service.updateClassMemberStatus("9", memberStatusRequest("ACTIVE"));

            // Then: Trạng thái của thành viên đó phải được lưu là ACTIVE
            assertNotNull(savedClassMember);
            assertEquals(ClassMemberStatus.ACTIVE, savedClassMember.getMemberStatus());
        }

        @Test
        @DisplayName("LH_LOP_26 - Đảm bảo cập nhật trạng thái thành viên xử lý đúng trường hợp lỗi: fail_throws when class member not found.")
        void updateClassMemberStatus_Fail_ThrowsWhenClassMemberNotFound() {
            mockCurrentUser(4L);

            when(classMemberRepository.findById(9L)).thenReturn(Optional.empty());

            assertThrows(AppException.class, () -> service.updateClassMemberStatus("9", memberStatusRequest("ACTIVE")));

            verify(classMemberRepository, never()).save(any(ClassMember.class));
        }

        @Test
        @DisplayName("LH_LOP_27 - Đảm bảo cập nhật trạng thái thành viên xử lý đúng trường hợp lỗi: fail_throws when current user is not teacher.")
        void updateClassMemberStatus_Fail_ThrowsWhenCurrentUserIsNotTeacher() {
            mockCurrentUser(4L);

            when(classMemberRepository.findById(9L))
                    .thenReturn(Optional.of(classMember(9L, 99L, ClassMemberStatus.INACTIVE)));

            assertThrows(AppException.class, () -> service.updateClassMemberStatus("9", memberStatusRequest("ACTIVE")));

            verify(classMemberRepository, never()).save(any(ClassMember.class));
        }

        @Test
        @DisplayName("LH_LOP_28 - Đảm bảo cập nhật trạng thái thành viên xử lý đúng trường hợp lỗi: fail_throws when request is null.")
        void updateClassMemberStatus_Fail_ThrowsWhenRequestIsNull() {
            mockCurrentUser(4L);

            assertThrows(NullPointerException.class, () -> service.updateClassMemberStatus("9", null));
        }
    }
}