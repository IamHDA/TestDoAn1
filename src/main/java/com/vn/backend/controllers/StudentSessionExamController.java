package com.vn.backend.controllers;

import com.vn.backend.annotation.AllowFormat;
import com.vn.backend.constants.AppConst;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.constants.AppConst.RegexConst;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.studentsessionexam.StudentSessionExamAddRequest;
import com.vn.backend.dto.request.studentsessionexam.StudentSessionExamSearchRequest;
import com.vn.backend.dto.response.common.AppResponse;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.studentsessionexam.AvailableStudentResponse;
import com.vn.backend.dto.response.studentsessionexam.StudentSessionExamResponse;
import com.vn.backend.services.StudentSessionExamService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping(AppConst.API + "/student-session-exams")
public class StudentSessionExamController extends BaseController {

    private final StudentSessionExamService studentSessionExamService;

    public StudentSessionExamController(StudentSessionExamService studentSessionExamService) {
        this.studentSessionExamService = studentSessionExamService;
    }

    @PostMapping("/search-class-students-for-session")
    public AppResponse<ResponseListData<AvailableStudentResponse>> searchClassStudentsForSessionExam(
            @RequestBody @Valid BaseFilterSearchRequest<StudentSessionExamSearchRequest> request) {
        log.info("Received request to search ALL students in class for session exam (with isJoined)");
        ResponseListData<AvailableStudentResponse> response = studentSessionExamService.searchClassStudentsForSessionExam(request);
        log.info("Successfully searched all students in class for session exam with join status");
        return success(response);
    }

    @PostMapping("/add")
    public AppResponse<List<StudentSessionExamResponse>> addStudents(
            @RequestBody @Valid StudentSessionExamAddRequest request) {
        log.info("Received request to add students to session exam");
        List<StudentSessionExamResponse> responses = studentSessionExamService.addStudents(request);
        log.info("Successfully added {} students to session exam with ID: {}",
                responses.size(), request.getSessionExamId());
        return success(null);
    }

    @DeleteMapping("/{sessionExamId}/students/{studentId}")
    public AppResponse<Void> removeStudent(
            @PathVariable
            @AllowFormat(regex = RegexConst.INTEGER, fieldName = FieldConst.SESSION_EXAM_ID_KEY, message = MessageConst.INVALID_NUMBER_FORMAT)
            String sessionExamId,
            @PathVariable
            @AllowFormat(regex = RegexConst.INTEGER, fieldName = "studentId", message = MessageConst.INVALID_NUMBER_FORMAT)
            String studentId) {
        log.info("Received request to remove student with ID: {} from session exam with ID: {}", studentId, sessionExamId);
        studentSessionExamService.removeStudent(Long.valueOf(sessionExamId), Long.valueOf(studentId));
        log.info("Successfully removed student with ID: {} from session exam with ID: {}", studentId, sessionExamId);
        return success(null);
    }



}

