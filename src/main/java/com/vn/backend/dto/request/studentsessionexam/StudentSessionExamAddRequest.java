package com.vn.backend.dto.request.studentsessionexam;

import com.vn.backend.annotation.NotAllowBlank;
import com.vn.backend.constants.AppConst;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class StudentSessionExamAddRequest {
    @NotAllowBlank(message = AppConst.MessageConst.REQUIRED_FIELD_EMPTY, fieldName = AppConst.FieldConst.SESSION_EXAM_ID_KEY)
    private Long sessionExamId;

    @NotEmpty(message = "Student IDs are required")
    private List<Long> studentIds;
}

