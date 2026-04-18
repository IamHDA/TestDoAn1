package com.vn.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ExamMode {
    FLEXIBLE,
    LIVE;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ExamMode from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return ExamMode.valueOf(value.toUpperCase());
    }
}


