package com.vn.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ClassroomStatus{
    ACTIVE,
    ARCHIVE;
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ClassroomStatus from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return ClassroomStatus.valueOf(value.toUpperCase());
    }
}
