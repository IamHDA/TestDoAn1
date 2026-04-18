package com.vn.backend.dto.request.exam;

import com.vn.backend.annotation.AllowFormat;
import com.vn.backend.annotation.NotAllowBlank;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.constants.AppConst.RegexConst;
import lombok.Data;

@Data
public class ExamUpdateRequest {

    @NotAllowBlank(message = MessageConst.REQUIRED_FIELD_EMPTY, fieldName = FieldConst.SUBJECT_ID)
    @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = FieldConst.SUBJECT_ID)
    private String subjectId;

    private String title;

    private String description;

    public ExamUpdateRequestDTO toDTO() {
        return ExamUpdateRequestDTO.builder()
                .subjectId(Long.parseLong(this.subjectId))
                .title(this.title)
                .description(this.description)
                .build();
    }
}
