package com.vn.backend.dto.request.classroom;

import com.vn.backend.annotation.AllowFormat;
import com.vn.backend.annotation.ValidEnum;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.constants.AppConst.RegexConst;
import com.vn.backend.enums.ClassroomStatus;
import com.vn.backend.utils.EnumUtils;
import com.vn.backend.utils.SearchUtils;
import lombok.Data;

@Data
public class ClassroomSearchRequest {

    private String teacherName;

    private String className;

    @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = FieldConst.SUBJECT_ID)
    private String subjectId;

    @ValidEnum(enumClass = ClassroomStatus.class, fieldName = FieldConst.CLASSROOM_STATUS, message = MessageConst.VALUE_OUT_OF_RANGE)
    private String classroomStatus;

    public ClassroomSearchRequestDTO toDTO() {
        return ClassroomSearchRequestDTO.builder()
                .teacherName(SearchUtils.getLikeValue(this.teacherName))
                .className(SearchUtils.getLikeValue(this.className))
                .subjectId((this.subjectId != null && !this.subjectId.isEmpty())
                        ? Long.parseLong(this.subjectId)
                        : null)
                .classroomStatus(EnumUtils.fromString(ClassroomStatus.class, this.classroomStatus))
                .build();
    }
}
