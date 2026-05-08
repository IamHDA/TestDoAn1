package com.vn.backend.selenium.tests;

import com.vn.backend.selenium.core.BaseSeleniumTest;
import com.vn.backend.selenium.core.TestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Selenium tests for module Quan ly lop hoc.
 *
 * Test design source:
 * - Unit Test sheet: Module QLLH, especially ClassroomServiceImplTest,
 *   InvitationServiceImplTest, AnnouncementServiceImplTest, CommentServiceImplTest,
 *   ClassroomStatisticsServiceImplTest.
 * - System Test sheet: Module QLLH, UI and functional flows.
 *
 * These tests map service-level happy/error paths to browser-level user flows.
 * Some tests require seeded data, e.g. at least one teacher class and one announcement.
 */
class QLLHSeleniumTest extends BaseSeleniumTest {

    @Test
    @DisplayName("QLLH-SEL-CLASS-01 - createClassroom_Success - Gui yeu cau tao lop")
    void createClassroom_Success_BasedOnUnitTest() {
        loginAsTeacher();
        ui.open("/classes");
        ui.waitText("Lớp học");

        String suffix = String.valueOf(System.currentTimeMillis()).substring(8);
        ui.clickButton("Tạo lớp học");
        ui.typeById("className", "Lớp Selenium " + suffix);
        ui.selectCustomOption("Chọn môn học", "Java");
        ui.typeById("description", "Lớp học dùng để kiểm thử Selenium");
        ui.clickButton("Thêm lịch học");
        ui.selectCustomOption("Chọn thứ", "Thứ 2");
        ui.typeByPlaceholder("00:00", "08:00");
        ui.appendByPlaceholder("00:00", "10:00");
        ui.typeByPlaceholder("VD: A101", "A101");
        ui.clickButton("Tạo lớp");

        ui.waitSuccessToast();
    }

    @Test
    @DisplayName("QLLH-SEL-CLASS-02 - createClassroom_Fail - Bo trong ten lop")
    void createClassroom_Fail_WhenClassNameMissing_BasedOnUnitTest() {
        loginAsTeacher();
        ui.open("/classes");

        ui.clickButton("Tạo lớp học");
        ui.clickButton("Tạo lớp");

        ui.waitErrorOrValidation();
    }

    @Test
    @DisplayName("QLLH-SEL-CLASS-03 - createClassroom_Fail - Ten lop qua dai")
    void createClassroom_Fail_WhenClassNameTooLong_BasedOnUnitTest() {
        loginAsTeacher();
        ui.open("/classes");

        ui.clickButton("Tạo lớp học");
        ui.typeById("className", "A".repeat(120));
        ui.typeById("description", "Lớp học dùng để kiểm thử validate");
        ui.clickButton("Tạo lớp");

        ui.waitErrorOrValidation();
    }

    @Test
    @DisplayName("QLLH-SEL-CLASS-04 - searchClassroom_Success - Tim kiem lop hoc")
    void searchClassroom_Success_BasedOnUnitTest() {
        loginAsTeacher();
        ui.open("/classes");
        ui.waitText("Lớp học");

        ui.typeById("className", TestConfig.EXISTING_CLASS_KEYWORD);
        ui.clickButton("Tìm kiếm");

        ui.waitText(TestConfig.EXISTING_CLASS_KEYWORD);
    }

    @Test
    @DisplayName("QLLH-SEL-CLASS-05 - updateClassroom_Success - Cap nhat thong tin lop")
    void updateClassroom_Success_BasedOnUnitTest() {
        loginAsTeacher();
        openFirstClassroomDetail();

        ui.clickText("Cài đặt");
        ui.clickButton("Chỉnh sửa");
        ui.clearById("className");
        ui.typeById("className", "Lớp đã cập nhật Selenium");
        ui.clearById("description");
        ui.typeById("description", "Mô tả đã cập nhật bằng Selenium");
        ui.clickButton("Cập nhật");

        ui.waitSuccessToast();
    }

    @Test
    @DisplayName("QLLH-SEL-CLASS-06 - updateClassroomSetting_Success - Cap nhat cai dat lop")
    void updateClassroomSetting_Success_BasedOnUnitTest() {
        loginAsTeacher();
        openFirstClassroomDetail();

        ui.clickText("Cài đặt");
        ui.waitText("Cài đặt lớp học");
        ui.click(By.id("allowStudentPost"));
        ui.click(By.id("notifyEmail"));
        ui.clickButton("Lưu");

        ui.waitSuccessToast();
    }

    @Test
    @DisplayName("QLLH-SEL-INVITE-01 - sendBulkInvitation_Success - Moi sinh vien")
    void sendBulkInvitation_Success_BasedOnUnitTest() {
        loginAsTeacher();
        openFirstClassroomDetail();

        ui.clickText("Mọi người");
        ui.clickButton("Mời thành viên");
        ui.selectCustomOption("Chọn vai trò", "Sinh viên");
        ui.typeByPlaceholder("Nhập email hoặc tên người dùng...", TestConfig.INVITE_STUDENT_KEYWORD);
        ui.clickFirstByText(TestConfig.INVITE_STUDENT_KEYWORD);
        ui.clickButton("Gửi lời mời");

        ui.waitSuccessToast();
    }

    @Test
    @DisplayName("QLLH-SEL-INVITE-02 - joinClassroomByCode_Success - Tham gia lop bang ma")
    void joinClassroomByCode_Success_BasedOnUnitTest() {
        loginAsStudent();
        ui.open("/classes");
        ui.waitText("Lớp học");

        ui.clickButton("Tham gia lớp học");
        ui.typeById("classCode", TestConfig.VALID_CLASS_CODE);
        ui.clickButton("Tham gia");

        ui.waitSuccessToast();
    }

    @Test
    @DisplayName("QLLH-SEL-INVITE-03 - joinClassroomByCode_Fail - Ma lop sai")
    void joinClassroomByCode_Fail_WhenCodeInvalid_BasedOnUnitTest() {
        loginAsStudent();
        ui.open("/classes");

        ui.clickButton("Tham gia lớp học");
        ui.typeById("classCode", "WRONG1");
        ui.clickButton("Tham gia");

        ui.waitErrorOrValidation();
    }

    @Test
    @DisplayName("QLLH-SEL-ANN-01 - createAnnouncement_Success - Tao bai dang")
    void createAnnouncement_Success_BasedOnUnitTest() {
        loginAsTeacher();
        openFirstClassroomDetail();

        ui.clickText("Bảng tin");
        ui.clickButton("Tạo bài đăng");
        ui.typeByPlaceholder("Nhập tiêu đề bài đăng", "Thông báo Selenium");
        ui.typeByPlaceholder("Nhập nội dung bài đăng", "Nội dung bài đăng được tạo bằng Selenium");
        ui.clickButton("Tạo bài đăng");

        ui.waitSuccessToast();
    }

    @Test
    @DisplayName("QLLH-SEL-ANN-02 - createAnnouncement_Fail - Thieu tieu de/noi dung")
    void createAnnouncement_Fail_WhenRequiredFieldsMissing_BasedOnUnitTest() {
        loginAsTeacher();
        openFirstClassroomDetail();

        ui.clickText("Bảng tin");
        ui.clickButton("Tạo bài đăng");
        ui.clickButton("Tạo bài đăng");

        ui.waitErrorOrValidation();
    }

    @Test
    @DisplayName("QLLH-SEL-ANN-03 - updateAnnouncement_Success - Sua bai dang")
    void updateAnnouncement_Success_BasedOnUnitTest() {
        loginAsTeacher();
        openFirstClassroomDetail();

        ui.clickText("Bảng tin");
        ui.clickFirstByText("Sửa bài đăng");
        ui.typeByPlaceholder("Nhập tiêu đề bài đăng", "Thông báo đã sửa bằng Selenium");
        ui.typeByPlaceholder("Nhập nội dung bài đăng", "Nội dung đã sửa bằng Selenium");
        ui.clickButton("Cập nhật bài đăng");

        ui.waitSuccessToast();
    }

    @Test
    @DisplayName("QLLH-SEL-ANN-04 - deleteAnnouncement_Success - Xoa bai dang")
    void deleteAnnouncement_Success_BasedOnUnitTest() {
        loginAsTeacher();
        openFirstClassroomDetail();

        ui.clickText("Bảng tin");
        ui.clickFirstByText("Xóa bài đăng");
        ui.optionalClickText("Xác nhận");

        ui.waitSuccessToast();
    }

    @Test
    @DisplayName("QLLH-SEL-COMMENT-01 - createComment_Success - Tao binh luan")
    void createComment_Success_BasedOnUnitTest() {
        loginAsStudent();
        openFirstClassroomDetail();

        ui.clickText("Bảng tin");
        ui.clickFirstByText("Xem chi tiết");
        ui.type(By.cssSelector("textarea, input[placeholder*='bình luận'], input[placeholder*='Bình luận']"), "Em đã xem thông báo ạ.");
        ui.clickButton("Gửi");

        ui.waitText("Em đã xem thông báo");
    }

    @Test
    @DisplayName("QLLH-SEL-COMMENT-02 - updateComment_Success - Sua binh luan cua minh")
    void updateComment_Success_BasedOnUnitTest() {
        loginAsStudent();
        openFirstClassroomDetail();

        ui.clickText("Bảng tin");
        ui.clickFirstByText("Xem chi tiết");
        ui.clickFirstByText("Sửa");
        ui.type(By.cssSelector("textarea, input[placeholder*='bình luận'], input[placeholder*='Bình luận']"), "Bình luận đã cập nhật");
        ui.clickButton("Lưu");

        ui.waitText("Bình luận đã cập nhật");
    }

    @Test
    @DisplayName("QLLH-SEL-COMMENT-03 - deleteComment_Success - Xoa binh luan")
    void deleteComment_Success_BasedOnUnitTest() {
        loginAsStudent();
        openFirstClassroomDetail();

        ui.clickText("Bảng tin");
        ui.clickFirstByText("Xem chi tiết");
        ui.clickFirstByText("Xóa");
        ui.optionalClickText("Xác nhận");

        ui.waitSuccessToast();
    }

    @Test
    @DisplayName("QLLH-SEL-STAT-01 - getAverageScoreComparison/getImprovementTrend - Xem thong ke lop")
    void classroomStatistics_Success_BasedOnUnitTest() {
        loginAsTeacher();
        openFirstClassroomDetail();

        ui.clickText("Điểm");
        ui.waitText("Thống kê");
    }

    private void openFirstClassroomDetail() {
        ui.open("/classes");
        ui.waitText("Lớp học");
        if (ui.hasText(TestConfig.EXISTING_CLASS_KEYWORD)) {
            ui.clickFirstByText(TestConfig.EXISTING_CLASS_KEYWORD);
        } else {
            ui.click(By.xpath("(//a[contains(@href,'/classes/')] | //div[contains(@class,'card')])[1]"));
        }
        ui.waitUrlContains("/classes/");
    }
}
