package com.vn.backend.dto.response.studentsessionexam;

import com.vn.backend.enums.ExamMode;

public interface StudentExamResultQueryDTO {
    Long getStudentSessionExamId();
    Double getScore();
    String getSubmissionResult();

    ExamMode getExamMode();
}
