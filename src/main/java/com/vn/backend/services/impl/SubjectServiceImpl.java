package com.vn.backend.services.impl;

import com.vn.backend.constants.AppConst.*;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.subject.SubjectCreateRequest;
import com.vn.backend.dto.request.subject.SubjectCreateRequestDTO;
import com.vn.backend.dto.request.subject.SubjectSearchRequest;
import com.vn.backend.dto.request.subject.SubjectSearchRequestDTO;
import com.vn.backend.dto.request.subject.UpdateSubjectRequest;
import com.vn.backend.dto.response.common.PagingMeta;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.subject.SubjectSearchResponse;
import com.vn.backend.entities.Subject;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.SubjectRepository;
import com.vn.backend.services.SubjectService;
import com.vn.backend.utils.MessageUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubjectServiceImpl extends BaseService implements SubjectService {

    private final SubjectRepository subjectRepository;

    public SubjectServiceImpl(MessageUtils messageUtils, SubjectRepository subjectRepository) {
        super(messageUtils);
        this.subjectRepository = subjectRepository;
    }

    @Override
    public void createSubject(SubjectCreateRequest request) {
        log.info("Start service to create subject");

        SubjectCreateRequestDTO dto = request.toDTO();
        if(subjectRepository.existsBySubjectCodeAndIsDeletedIsFalse(dto.getSubjectCode())){
            throw new AppException(MessageConst.SUBJECT_CODE_ALREADY_EXISTS, messageUtils.getMessage(MessageConst.SUBJECT_CODE_ALREADY_EXISTS), HttpStatus.BAD_REQUEST);
        }
        Subject subject = Subject.builder()
                .subjectCode(dto.getSubjectCode())
                .subjectName(dto.getSubjectName())
                .build();
        subjectRepository.save(subject);

        log.info("End service create subject");
    }

    @Override
    public ResponseListData<SubjectSearchResponse> searchSubject(BaseFilterSearchRequest<SubjectSearchRequest> request) {
        log.info("Start service to search subject");

        SubjectSearchRequestDTO dto = request.getFilters().toDTO();
        Pageable pageable = request.getPagination().getPagingMeta().toPageable();
        Page<Subject> subjectPage = subjectRepository.searchSubject(dto, pageable);
        List<SubjectSearchResponse> responseList = subjectPage.stream()
                .map(SubjectSearchResponse::fromEntity)
                .toList();
        PagingMeta pagingMeta = request.getPagination().getPagingMeta();
        pagingMeta.setTotalRows(subjectPage.getTotalElements());
        pagingMeta.setTotalPages(subjectPage.getTotalPages());

        log.info("End service search subject");
        return new ResponseListData<>(responseList, pagingMeta);
    }

    @Override
    public void updateSubject(Long subjectId, UpdateSubjectRequest request) {
        log.info("Start service to update subject: {}", subjectId);
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new AppException(MessageConst.NOT_FOUND, messageUtils.getMessage(MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));
        // Kiểm tra trùng mã nếu đổi mã
        if (request.getSubjectCode() != null && !request.getSubjectCode().equals(subject.getSubjectCode())) {
            if (subjectRepository.existsBySubjectCodeAndIsDeletedIsFalse(request.getSubjectCode())) {
                throw new AppException(MessageConst.SUBJECT_CODE_ALREADY_EXISTS, messageUtils.getMessage(MessageConst.SUBJECT_CODE_ALREADY_EXISTS), HttpStatus.BAD_REQUEST);
            }
            subject.setSubjectCode(request.getSubjectCode());
        }
        if (request.getSubjectName() != null) {
            subject.setSubjectName(request.getSubjectName());
        }
        subjectRepository.save(subject);
        log.info("End service update subject: {}", subjectId);
    }

    @Override
    public void deleteSubject(Long subjectId) {
        log.info("Start service to delete subject: {}", subjectId);
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new AppException(MessageConst.NOT_FOUND, messageUtils.getMessage(MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));
        subject.setIsDeleted(true);
        subjectRepository.save(subject);
        log.info("End service delete subject: {}", subjectId);
    }
}
