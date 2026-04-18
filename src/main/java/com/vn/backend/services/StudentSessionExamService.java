package com.vn.backend.services;

import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.studentsessionexam.StudentSessionExamAddRequest;
import com.vn.backend.dto.request.studentsessionexam.StudentSessionExamSearchRequest;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.studentsessionexam.AvailableStudentResponse;
import com.vn.backend.dto.response.studentsessionexam.StudentSessionExamResponse;

import java.util.List;

public interface StudentSessionExamService {
    ResponseListData<AvailableStudentResponse> searchClassStudentsForSessionExam(BaseFilterSearchRequest<StudentSessionExamSearchRequest> request);

    List<StudentSessionExamResponse> addStudents(StudentSessionExamAddRequest request);

    void removeStudent(Long sessionExamId, Long studentId);
}

