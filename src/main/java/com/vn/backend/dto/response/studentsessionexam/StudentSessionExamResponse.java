package com.vn.backend.dto.response.studentsessionexam;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentSessionExamResponse {
    private Long studentSessionExamId;
    private Long sessionExamId;
    private Long studentId;
    private String studentFullName;
    private String studentUsername;
    private String studentCode;
    private String studentEmail;
    private String studentAvatarUrl;
}

