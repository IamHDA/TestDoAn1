package com.vn.backend.dto.response.sessionexam;

import com.vn.backend.enums.ExamMode;
import com.vn.backend.enums.QuestionOrderMode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SessionExamResponse {
    private Long sessionExamId;
    private Long classId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String title;
    private String description;
    private Long duration;
    private Long examId;
    private ExamMode examMode;
    private Boolean isProctored;
    private QuestionOrderMode questionOrderMode;
    private Boolean isInstantlyResult;
    private Long createdBy;
}


