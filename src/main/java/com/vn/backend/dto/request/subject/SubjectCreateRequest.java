package com.vn.backend.dto.request.subject;

import com.vn.backend.annotation.NotAllowBlank;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import lombok.Data;

@Data
public class SubjectCreateRequest {

    @NotAllowBlank(message = MessageConst.REQUIRED_FIELD_EMPTY, fieldName = FieldConst.SUBJECT_CODE)
    private String subjectCode;
    @NotAllowBlank(message = MessageConst.REQUIRED_FIELD_EMPTY, fieldName = FieldConst.SUBJECT_NAME)
    private String subjectName;

    public SubjectCreateRequestDTO toDTO() {
        return SubjectCreateRequestDTO.builder()
                .subjectCode(this.subjectCode)
                .subjectName(this.subjectName)
                .build();
    }
}
