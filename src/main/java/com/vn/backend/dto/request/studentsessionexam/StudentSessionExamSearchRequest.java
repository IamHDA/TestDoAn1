package com.vn.backend.dto.request.studentsessionexam;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentSessionExamSearchRequest {
    private Long sessionExamId;
    private String search;
}

