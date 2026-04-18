package com.vn.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
public enum StatusUser {
    ACTIVE(true,"ACTIVE","Trạng thái còn hoạt động"),
    INACTIVE(false,"INACTIVE","Trạng thái không hoạt động");
    private Boolean value;
    private String name;
    private String description;
    StatusUser(Boolean value, String name, String description) {
        this.name = name;
        this.value = value;
        this.description = description;
    }
    public String getName() {
        return name();
    }
    public static StatusUser fromName(String name) {
        return StatusUser.valueOf(name);
    }
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static StatusUser from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return StatusUser.valueOf(value.toUpperCase());
    }

}
