package com.vn.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum AnnouncementType {
    MATERIAL,      // up lên 1 tài liệu
    ASSIGNMENT,    // up lên 1 bài tập
    EXAM,          // up lên 1 bài kiểm tra
    GENERIC;        // đăng 1 thông báo chung
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static AnnouncementType from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return AnnouncementType.valueOf(value.toUpperCase());
    }
}
