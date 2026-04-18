package com.vn.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ClassMember {
    // declined/pending/active/inactive
    DECLINED("Sinh viên từ chối lời mời"),
    PENDING("Trong thời gian chờ lời mời"),
    ACTIVE("Sinh viên hoạt động trong lớp học"),
    INACTIVE("Sinh viên bị đuổi khỏi lớp học");
    public final String description;
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ClassMember from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return ClassMember.valueOf(value.toUpperCase());
    }
    private ClassMember(String description) {
        this.description=description;
    }
}
