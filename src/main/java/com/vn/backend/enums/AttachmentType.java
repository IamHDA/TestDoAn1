package com.vn.backend.enums;

public enum AttachmentType {
    ASSIGNMENT,     // File đính kèm cho bài tập
    GENERIC,        // File đính kèm cho thông báo chung
    MATERIAL,       // File đính kèm cho tài liệu
    EXAM,           // File đính kèm cho bài kiểm tra
    SUBMISSION;      // File nộp bài của học sinh
    public static AttachmentType mapToAttachmentType(AnnouncementType announcementType) {
        return switch (announcementType) {
            case GENERIC -> AttachmentType.GENERIC;
            case ASSIGNMENT -> AttachmentType.ASSIGNMENT;
            case EXAM -> AttachmentType.EXAM;
            case MATERIAL -> AttachmentType.MATERIAL;
        };
    }
}
