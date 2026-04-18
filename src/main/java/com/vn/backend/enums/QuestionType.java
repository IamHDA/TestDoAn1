package com.vn.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public enum QuestionType {
    MULTI_CHOICE,
    SINGLE_CHOICE;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static QuestionType from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return QuestionType.valueOf(value.toUpperCase());
    }
}
