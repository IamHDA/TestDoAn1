package com.vn.backend.dto.request.sessionexam;

import com.vn.backend.enums.ExamMode;
import com.vn.backend.enums.QuestionOrderMode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SessionExamUpdateRequest {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String title;
    private String description;
    private Long duration;
    private ExamMode examMode;
    private QuestionOrderMode questionOrderMode;
    private Boolean isInstantlyResult;
}


