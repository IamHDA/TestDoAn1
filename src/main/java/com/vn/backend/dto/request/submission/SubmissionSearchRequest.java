package com.vn.backend.dto.request.submission;

import com.vn.backend.annotation.AllowFormat;
import com.vn.backend.constants.AppConst.*;
import com.vn.backend.enums.GradingStatus;
import com.vn.backend.enums.SubmissionStatus;
import com.vn.backend.utils.EnumUtils;
import com.vn.backend.utils.SearchUtils;
import lombok.Data;

@Data
public class SubmissionSearchRequest {
    private String keyword;
    private String submissionStatus;
    private String gradingStatus;

    @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = FieldConst.ASSIGNMENT_ID)
    private String assignmentId;

    public SubmissionSearchRequestDTO toDTO() {
        return SubmissionSearchRequestDTO.builder()
                .username(SearchUtils.getLikeValue(this.keyword))
                .fullName(SearchUtils.getLikeValue(this.keyword))
                .submissionStatus(EnumUtils.fromString(SubmissionStatus.class, this.submissionStatus))
                .gradingStatus(EnumUtils.fromString(GradingStatus.class, this.gradingStatus))
                .assignmentId(Long.parseLong(this.assignmentId))
                .build();
    }
}
