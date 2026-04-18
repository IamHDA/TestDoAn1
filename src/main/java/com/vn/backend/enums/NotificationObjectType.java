package com.vn.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum NotificationObjectType {
    INVITE_CLASS,       // Lời mời tham gia lớp học
    JOIN_CLASS,         // Yêu cầu tham gia lớp học được chấp thuận hoặc có người mới vào
    ANNOUNCEMENT,       // Thông báo chung từ giáo viên
    ASSIGNMENT,         // Có bài tập mới hoặc cập nhật
    EXAM_JOINED,        // Tham gia kỳ thi
    EXAM_CREATED,       // Kỳ thi mới được tạo
    MATERIAL,           // Tài liệu mới
    
    // Notification types for content management
    TOPIC_CREATED,      // Topic mới được tạo
    TOPIC_APPROVED,     // Topic được duyệt
    TOPIC_REJECTED,     // Topic bị từ chối
    QUESTION_CREATED,   // Câu hỏi mới được tạo
    QUESTION_APPROVED,  // Câu hỏi được duyệt
    QUESTION_REJECTED,  // Câu hỏi bị từ chối
    CLASS_CREATED,      // Lớp học mới được tạo
    CLASS_APPROVED,     // Lớp học được duyệt
    CLASS_REJECTED,     // Lớp học bị từ chối
    SYSTEM_NOTIFICATION; // Thông báo hệ thống
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static NotificationObjectType from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return NotificationObjectType.valueOf(value.toUpperCase());
    }
}
