package com.vn.backend.dto.request.exam;

import com.vn.backend.annotation.AllowFormat;
import com.vn.backend.annotation.NotAllowBlank;
import com.vn.backend.constants.AppConst;
import lombok.Data;

@Data
public class ExamCreateRequest {

    @NotAllowBlank(message = AppConst.MessageConst.REQUIRED_FIELD_EMPTY, fieldName = AppConst.FieldConst.SUBJECT_ID)
    @AllowFormat(regex = AppConst.RegexConst.INTEGER, message = AppConst.MessageConst.INVALID_NUMBER_FORMAT, fieldName = AppConst.FieldConst.SUBJECT_ID)
    private String subjectId;

    private String title;

    private String description;

    public ExamCreateRequestDTO toDTO() {
        return ExamCreateRequestDTO.builder()
                .subjectId(Long.parseLong(this.subjectId))
                .title(this.title)
                .description(this.description)
                .build();
    }
}
