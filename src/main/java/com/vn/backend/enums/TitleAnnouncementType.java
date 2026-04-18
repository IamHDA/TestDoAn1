package com.vn.backend.enums;

public enum TitleAnnouncementType {
    EXAM,
    ASSIGNMENT,
    GENERIC,
    MATERIAL;

    public static TitleAnnouncementType getType(AnnouncementType announcementType) {
        return switch (announcementType) {
            case ASSIGNMENT -> TitleAnnouncementType.ASSIGNMENT;
            case EXAM -> TitleAnnouncementType.EXAM;
            case MATERIAL -> TitleAnnouncementType.MATERIAL;
            default -> TitleAnnouncementType.GENERIC;
        };
    }
}
