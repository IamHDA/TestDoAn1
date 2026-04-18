package com.vn.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ClassMemberStatus {
    ACTIVE,
    INACTIVE;
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ClassMemberStatus from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return ClassMemberStatus.valueOf(value.toUpperCase());
    }
}
