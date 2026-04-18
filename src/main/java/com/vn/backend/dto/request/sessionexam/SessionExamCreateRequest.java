package com.vn.backend.dto.request.sessionexam;

import com.vn.backend.annotation.NotAllowBlank;
import com.vn.backend.constants.AppConst;
import com.vn.backend.enums.ExamMode;
import com.vn.backend.enums.QuestionOrderMode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SessionExamCreateRequest {
    @NotAllowBlank(message = AppConst.MessageConst.REQUIRED_FIELD_EMPTY, fieldName = AppConst.FieldConst.CLASSROOM_ID)
    private Long classId;
    @NotAllowBlank(message = AppConst.MessageConst.REQUIRED_FIELD_EMPTY, fieldName = AppConst.FieldConst.EXAM_ID)
    private Long examId;
    @NotAllowBlank(message = AppConst.MessageConst.REQUIRED_FIELD_EMPTY, fieldName = AppConst.FieldConst.START_DATE)
    private LocalDateTime startDate;
    @NotAllowBlank(message = AppConst.MessageConst.REQUIRED_FIELD_EMPTY, fieldName = AppConst.FieldConst.END_DATE)
    private LocalDateTime endDate;
    private String title;
    private String description;
    private Long duration;
    private ExamMode examMode;
    private QuestionOrderMode questionOrderMode;
    private Boolean isInstantlyResult;
}


