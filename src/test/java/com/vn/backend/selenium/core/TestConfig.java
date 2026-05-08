package com.vn.backend.selenium.core;

public final class TestConfig {
    private TestConfig() {}

    public static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:5173");

    public static final String ADMIN_USERNAME = System.getProperty("admin.username", "admin");
    public static final String ADMIN_PASSWORD = System.getProperty("admin.password", "123456");

    public static final String TEACHER_USERNAME = System.getProperty("teacher.username", "teacher01");
    public static final String TEACHER_PASSWORD = System.getProperty("teacher.password", "123456");

    public static final String STUDENT_USERNAME = System.getProperty("student.username", "student01");
    public static final String STUDENT_PASSWORD = System.getProperty("student.password", "123456");

    public static final String INVITE_STUDENT_KEYWORD = System.getProperty("invite.student.keyword", "student");
    public static final String VALID_CLASS_CODE = System.getProperty("class.code", "ABC123");
    public static final String EXISTING_CLASS_KEYWORD = System.getProperty("class.keyword", "SE");
}
