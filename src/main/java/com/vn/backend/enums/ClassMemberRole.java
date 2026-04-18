package com.vn.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ClassMemberRole {
    STUDENT,
    ASSISTANT;
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ClassMemberRole from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return ClassMemberRole.valueOf(value.toUpperCase());
    }
}
