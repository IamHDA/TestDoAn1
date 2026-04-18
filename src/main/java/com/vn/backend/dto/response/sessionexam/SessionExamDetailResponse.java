package com.vn.backend.dto.response.sessionexam;

import com.vn.backend.enums.ExamMode;
import com.vn.backend.enums.QuestionOrderMode;
import com.vn.backend.enums.SessionExamStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SessionExamDetailResponse {
    private Long sessionExamId;
    private Long classId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String title;
    private String description;
    private Long duration;
    private ExamMode examMode;
    private SessionExamStatus status;
    private Boolean isProctored;
    private QuestionOrderMode questionOrderMode;
    private Boolean isInstantlyResult;
    private Long createdBy;
    private Long examId;

    private String examTitle;
    private String examDescription;
    private Long examCreatedBy;
    private String examCreatorFullName;
    private String examCreatorEmail;
    private Long subjectId;
    private String subjectName;
    private String subjectCode;
}


