package com.vn.backend.selenium.tests;

import com.vn.backend.selenium.core.BaseSeleniumTest;
import com.vn.backend.selenium.core.ByHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Selenium tests for module Quan ly he thong.
 *
 * Test design source:
 * - Unit Test sheet: Module QLHT, especially UserServiceImplTest, SubjectServiceImplTest,
 *   ApprovalRequestServiceImplTest.
 * - System Test sheet: Module QLHT, UI and functional flows.
 *
 * These tests intentionally validate UI-level behavior that maps to service-level unit tests.
 * Some tests require seeded data, e.g. pending approval request, existing user/subject.
 */
class QLHTSeleniumTest extends BaseSeleniumTest {

    @Test
    @DisplayName("QLHT-SEL-USER-01 - createUser_Success - Tao user hop le")
    void createUser_Success_BasedOnUnitTest() {
        loginAsAdmin();
        ui.open("/admin/users");
        ui.waitText("Quản lý người dùng");

        String suffix = String.valueOf(System.currentTimeMillis());
        ui.clickButton("Thêm người dùng");
        ui.typeById("code", "STU" + suffix.substring(suffix.length() - 6));
        ui.typeById("username", "student" + suffix);
        ui.typeById("fullName", "Nguyen Van Selenium");
        ui.typeById("email", "student" + suffix + "@vn.com");
        ui.typeById("password", "password");
        ui.selectCustomOption("Chọn vai trò", "Student");
        ui.clickButton("Thêm mới");

        ui.waitSuccessToast();
        assertTrue(driver.findElement(By.tagName("body")).getText().contains("student" + suffix));
    }

    @Test
    @DisplayName("QLHT-SEL-USER-02 - createUser_Fail - Email sai dinh dang")
    void createUser_Fail_WhenEmailInvalid() {
        loginAsAdmin();
        ui.open("/admin/users");

        String suffix = String.valueOf(System.currentTimeMillis());
        ui.clickButton("Thêm người dùng");
        ui.typeById("code", "STU" + suffix.substring(suffix.length() - 6));
        ui.typeById("username", "student" + suffix);
        ui.typeById("fullName", "Nguyen Van Selenium");
        ui.typeById("email", "invalid-email");
        ui.typeById("password", "password");
        ui.selectCustomOption("Chọn vai trò", "Student");
        ui.clickButton("Thêm mới");

        ui.waitErrorOrValidation();
    }

    @Test
    @DisplayName("QLHT-SEL-USER-03 - createUser_Fail - Du lieu bat buoc bi bo trong")
    void createUser_Fail_WhenRequiredFieldsMissing() {
        loginAsAdmin();
        ui.open("/admin/users");

        ui.clickButton("Thêm người dùng");
        ui.clickButton("Thêm mới");

        ui.waitErrorOrValidation();
    }

    @Test
    @DisplayName("QLHT-SEL-USER-04 - createUser_Fail - Full name/email qua dai")
    void createUser_Fail_WhenInputTooLong() {
        loginAsAdmin();
        ui.open("/admin/users");

        String suffix = String.valueOf(System.currentTimeMillis());
        ui.clickButton("Thêm người dùng");
        ui.typeById("code", "STU" + suffix.substring(suffix.length() - 6));
        ui.typeById("username", "student" + suffix);
        ui.typeById("fullName", "A".repeat(80));
        ui.typeById("email", "a".repeat(321));
        ui.typeById("password", "password");
        ui.selectCustomOption("Chọn vai trò", "Student");
        ui.clickButton("Thêm mới");

        ui.waitErrorOrValidation();
    }

    @Test
    @DisplayName("QLHT-SEL-USER-05 - updateUser_Success - Cap nhat user")
    void updateUser_Success_BasedOnUnitTest() {
        loginAsAdmin();
        ui.open("/admin/users");
        ui.waitText("Quản lý người dùng");

        ui.clickFirstByText("Sửa");
        ui.clearById("fullName");
        ui.typeById("fullName", "Updated Selenium User");
        ui.clickButton("Cập nhật");

        ui.waitSuccessToast();
    }

    @Test
    @DisplayName("QLHT-SEL-USER-06 - updateUserStatus_Success - Khoa/mo khoa user")
    void updateUserStatus_Success_BasedOnUnitTest() {
        loginAsAdmin();
        ui.open("/admin/users");
        ui.waitText("Quản lý người dùng");

        ui.clickFirstByText("Khóa");
        ui.optionalClickText("Xác nhận");

        ui.waitSuccessToast();
    }

    @Test
    @DisplayName("QLHT-SEL-SUBJECT-01 - createSubject_Success - Tao mon hoc")
    void createSubject_Success_BasedOnUnitTest() {
        loginAsAdmin();
        ui.open("/admin/subjects");
        ui.waitText("Quản lý môn học");

        String suffix = String.valueOf(System.currentTimeMillis()).substring(8);
        ui.clickButton("Thêm môn học");
        ui.typeByPlaceholder("Nhập tên môn học", "Selenium Subject " + suffix);
        ui.typeByPlaceholder("Nhập mã môn học", "SEL" + suffix);
        ui.clickButton("Tạo mới");

        ui.waitSuccessToast();
        assertTrue(driver.findElement(By.tagName("body")).getText().contains("SEL" + suffix));
    }

    @Test
    @DisplayName("QLHT-SEL-SUBJECT-02 - createSubject_Fail - Thieu ten/ma mon hoc")
    void createSubject_Fail_WhenRequiredFieldsMissing() {
        loginAsAdmin();
        ui.open("/admin/subjects");

        ui.clickButton("Thêm môn học");
        ui.clickButton("Tạo mới");

        ui.waitErrorOrValidation();
    }

    @Test
    @DisplayName("QLHT-SEL-SUBJECT-03 - createSubject_Fail - Ten mon hoc qua dai")
    void createSubject_Fail_WhenSubjectNameTooLong() {
        loginAsAdmin();
        ui.open("/admin/subjects");

        ui.clickButton("Thêm môn học");
        ui.typeByPlaceholder("Nhập tên môn học", "A".repeat(120));
        ui.typeByPlaceholder("Nhập mã môn học", "LONG101");
        ui.clickButton("Tạo mới");

        ui.waitErrorOrValidation();
    }

    @Test
    @DisplayName("QLHT-SEL-SUBJECT-04 - updateSubject_Success - Cap nhat mon hoc")
    void updateSubject_Success_BasedOnUnitTest() {
        loginAsAdmin();
        ui.open("/admin/subjects");
        ui.waitText("Quản lý môn học");

        ui.clickFirstByText("Sửa");
        ui.typeByPlaceholder("Nhập tên môn học", "Selenium Updated Subject");
        ui.clickButton("Cập nhật");

        ui.waitSuccessToast();
    }

    @Test
    @DisplayName("QLHT-SEL-APPROVAL-01 - getApprovalRequestDetail_Success - Xem chi tiet yeu cau")
    void getApprovalRequestDetail_Success_BasedOnUnitTest() {
        loginAsAdmin();
        ui.open("/admin/requests");
        ui.waitText("Yêu cầu");

        ui.clickFirstByText("Xem");
        ui.waitText("Chi tiết");
    }

    @Test
    @DisplayName("QLHT-SEL-APPROVAL-02 - approveRequest_Success - Phe duyet yeu cau")
    void approveRequest_Success_BasedOnUnitTest() {
        loginAsAdmin();
        ui.open("/admin/requests");
        ui.waitText("Yêu cầu");

        ui.clickFirstByText("Xem");
        ui.clickButton("Phê duyệt");
        ui.optionalClickText("Xác nhận");

        ui.waitSuccessToast();
    }

    @Test
    @DisplayName("QLHT-SEL-APPROVAL-03 - rejectRequest_Fail - Tu choi khong nhap ly do")
    void rejectRequest_Fail_WhenRejectReasonBlank_BasedOnUnitTest() {
        loginAsAdmin();
        ui.open("/admin/requests");
        ui.waitText("Yêu cầu");

        ui.clickFirstByText("Xem");
        ui.clickButton("Từ chối");
        ui.clickButton("Xác nhận");

        ui.waitErrorOrValidation();
    }

    @Test
    @DisplayName("QLHT-SEL-APPROVAL-04 - rejectRequest_Success - Tu choi co ly do")
    void rejectRequest_Success_BasedOnUnitTest() {
        loginAsAdmin();
        ui.open("/admin/requests");
        ui.waitText("Yêu cầu");

        ui.clickFirstByText("Xem");
        ui.clickButton("Từ chối");
        ui.typeById("reject-reason", "Không đủ thông tin để phê duyệt");
        ui.clickButton("Xác nhận");

        ui.waitSuccessToast();
    }
}
