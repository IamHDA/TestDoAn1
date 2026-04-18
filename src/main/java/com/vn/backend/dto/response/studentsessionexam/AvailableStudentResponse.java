package com.vn.backend.dto.response.studentsessionexam;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AvailableStudentResponse {
    private Long studentId;
    private String fullName;
    private String username;
    private String code;
    private String email;
    private String avatarUrl;
    private boolean isJoined;
}

