package com.vn.backend.dto.request.invitation;

import com.vn.backend.annotation.AllowLength;
import com.vn.backend.annotation.NotAllowBlank;
import com.vn.backend.constants.AppConst;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinClassroomByCodeRequest {

    @NotNull(message = "Class code is required")
    @NotAllowBlank(message = "Class code cannot be blank")
    @AllowLength(min = 6, max = 6, message = AppConst.MessageConst.CLASS_CODE_LENGTH_INVALID, fieldName = AppConst.FieldConst.CLASS_CODE)
    private String classCode;
}
