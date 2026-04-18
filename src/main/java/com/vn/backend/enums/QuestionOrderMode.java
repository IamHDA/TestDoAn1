package com.vn.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum QuestionOrderMode {
    RANDOM,
    SEQUENTIAL,
    DIFFICULTY_ASC,
    DIFFICULTY_DESC;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static QuestionOrderMode from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return QuestionOrderMode.valueOf(value.toUpperCase());
    }
}


