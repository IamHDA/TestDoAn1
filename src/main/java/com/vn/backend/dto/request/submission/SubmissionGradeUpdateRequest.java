package com.vn.backend.dto.request.submission;

import com.vn.backend.annotation.AllowFormat;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.constants.AppConst.RegexConst;
import lombok.Builder;
import lombok.Data;

@Data
public class SubmissionGradeUpdateRequest {
    @AllowFormat(regex = RegexConst.NON_NEGATIVE_DECIMAL_2, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = FieldConst.GRADE)
    private String grade;

    public SubmissionGradeUpdateRequestDTO toDTO(){
        return SubmissionGradeUpdateRequestDTO.builder()
                .grade(Double.parseDouble(this.grade))
                .build();
    }
}
