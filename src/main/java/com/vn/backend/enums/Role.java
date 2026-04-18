package com.vn.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public enum Role {
    STUDENT,
    ADMIN,
    TEACHER;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Role from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Role.valueOf(value.toUpperCase());
    }

    public static Set<String> allowedNames() {
        EnumSet<Role> all = EnumSet.allOf(Role.class);
        Set<String> names = new HashSet<>();
        for (Role r : all) {
            names.add(r.name());
        }
        return Collections.unmodifiableSet(names);
    }
}
