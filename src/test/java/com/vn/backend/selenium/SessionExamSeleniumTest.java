package com.vn.backend.selenium;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Selenium Test: Kiểm tra Session Exam API qua Swagger UI.
 *
 * Session Exam (/api/session-exams) là module quan trọng nhất (ca thi trực tiếp):
 *   POST   /create                        – Tạo ca thi
 *   PUT    /update/{id}                   – Cập nhật ca thi
 *   GET    /detail/{id}                   – Xem chi tiết ca thi
 *   DELETE /delete/{id}                   – Xóa ca thi
 *   POST   /teacher/search                – Giáo viên tìm ca thi
 *   POST   /student/search                – Học sinh tìm ca thi
 *   GET    /exam-questions/{id}           – Lấy danh sách câu hỏi
 *   POST   /{id}/join                     – Vào phòng thi
 *   GET    /{id}/download                 – Tải đề thi
 *   POST   /{id}/save                     – Lưu câu trả lời
 *   POST   /{id}/submit                   – Nộp bài
 *   GET    /{id}/monitoring               – Giám sát phòng thi
 *   GET    /{id}/descriptive-statistic    – Thống kê mô tả
 *   GET    /result/{studentSessionExamId} – Xem kết quả thi
 *
 * Test Cases: TC_SESSION_01 → TC_SESSION_14
 */
@DisplayName("Session Exam API - Selenium Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SessionExamSeleniumTest extends SeleniumBaseTest {

    private static final String SWAGGER_PATH = "/swagger-ui/index.html";
    private static final int LOAD_WAIT = 20;

    private WebDriverWait openAndExpand() throws InterruptedException {
        navigateTo(SWAGGER_PATH);
        WebDriverWait wait = createWait(LOAD_WAIT);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".opblock-tag")));
        for (WebElement tag : driver.findElements(By.cssSelector(".opblock-tag"))) {
            try {
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].click();", tag.findElement(By.tagName("a")));
                Thread.sleep(150);
            } catch (Exception ignored) {}
        }
        Thread.sleep(1000);
        return wait;
    }

    @Test @Order(1)
    @DisplayName("TC_SESSION_01 - POST /api/session-exams/create endpoint tồn tại")
    void TC_SESSION_01_createSessionExamEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> paths = driver.findElements(By.cssSelector(".opblock-summary-path"));
        boolean found = paths.stream().anyMatch(p -> p.getText().contains("/session-exams/create"));
        System.out.println("[TC_SESSION_01] /session-exams/create found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(2)
    @DisplayName("TC_SESSION_02 - PUT /api/session-exams/update/{id} endpoint tồn tại")
    void TC_SESSION_02_updateSessionExamEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> putBlocks = driver.findElements(By.cssSelector(".opblock.opblock-put"));
        boolean found = putBlocks.stream()
                .anyMatch(b -> b.getText().contains("/session-exams/update/"));
        System.out.println("[TC_SESSION_02] PUT /session-exams/update found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(3)
    @DisplayName("TC_SESSION_03 - GET /api/session-exams/detail/{id} endpoint tồn tại")
    void TC_SESSION_03_getSessionExamDetailEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> getBlocks = driver.findElements(By.cssSelector(".opblock.opblock-get"));
        boolean found = getBlocks.stream()
                .anyMatch(b -> b.getText().contains("/session-exams/detail/"));
        System.out.println("[TC_SESSION_03] GET /session-exams/detail found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(4)
    @DisplayName("TC_SESSION_04 - DELETE /api/session-exams/delete/{id} endpoint tồn tại")
    void TC_SESSION_04_deleteSessionExamEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> delBlocks = driver.findElements(By.cssSelector(".opblock.opblock-delete"));
        boolean found = delBlocks.stream()
                .anyMatch(b -> b.getText().contains("/session-exams/delete/"));
        System.out.println("[TC_SESSION_04] DELETE /session-exams/delete found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(5)
    @DisplayName("TC_SESSION_05 - POST /api/session-exams/teacher/search endpoint tồn tại")
    void TC_SESSION_05_teacherSearchSessionExamEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> paths = driver.findElements(By.cssSelector(".opblock-summary-path"));
        boolean found = paths.stream().anyMatch(p -> p.getText().contains("/session-exams/teacher/search"));
        System.out.println("[TC_SESSION_05] /session-exams/teacher/search found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(6)
    @DisplayName("TC_SESSION_06 - POST /api/session-exams/student/search endpoint tồn tại")
    void TC_SESSION_06_studentSearchSessionExamEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> paths = driver.findElements(By.cssSelector(".opblock-summary-path"));
        boolean found = paths.stream().anyMatch(p -> p.getText().contains("/session-exams/student/search"));
        System.out.println("[TC_SESSION_06] /session-exams/student/search found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(7)
    @DisplayName("TC_SESSION_07 - POST /api/session-exams/{id}/join (vào phòng thi) tồn tại")
    void TC_SESSION_07_joinSessionExamEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> postBlocks = driver.findElements(By.cssSelector(".opblock.opblock-post"));
        boolean found = postBlocks.stream()
                .anyMatch(b -> b.getText().contains("session-exams") && b.getText().contains("join"));
        System.out.println("[TC_SESSION_07] POST /session-exams/{id}/join found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(8)
    @DisplayName("TC_SESSION_08 - GET /api/session-exams/{id}/download (tải đề thi) tồn tại")
    void TC_SESSION_08_downloadExamEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> getBlocks = driver.findElements(By.cssSelector(".opblock.opblock-get"));
        boolean found = getBlocks.stream()
                .anyMatch(b -> b.getText().contains("session-exams") && b.getText().contains("download"));
        System.out.println("[TC_SESSION_08] GET /session-exams/{id}/download found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(9)
    @DisplayName("TC_SESSION_09 - POST /api/session-exams/{id}/submit (nộp bài) tồn tại")
    void TC_SESSION_09_submitExamEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> postBlocks = driver.findElements(By.cssSelector(".opblock.opblock-post"));
        boolean found = postBlocks.stream()
                .anyMatch(b -> b.getText().contains("session-exams") && b.getText().contains("submit"));
        System.out.println("[TC_SESSION_09] POST /session-exams/{id}/submit found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(10)
    @DisplayName("TC_SESSION_10 - GET /api/session-exams/{id}/monitoring (giám sát) tồn tại")
    void TC_SESSION_10_examMonitoringEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> getBlocks = driver.findElements(By.cssSelector(".opblock.opblock-get"));
        boolean found = getBlocks.stream()
                .anyMatch(b -> b.getText().contains("session-exams") && b.getText().contains("monitoring"));
        System.out.println("[TC_SESSION_10] GET monitoring found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(11)
    @DisplayName("TC_SESSION_11 - GET /api/session-exams/{id}/descriptive-statistic tồn tại")
    void TC_SESSION_11_descriptiveStatisticEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> getBlocks = driver.findElements(By.cssSelector(".opblock.opblock-get"));
        boolean found = getBlocks.stream()
                .anyMatch(b -> b.getText().contains("descriptive-statistic"));
        System.out.println("[TC_SESSION_11] GET descriptive-statistic found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(12)
    @DisplayName("TC_SESSION_12 - GET /api/session-exams/result/{id} (kết quả thi) tồn tại")
    void TC_SESSION_12_studentExamResultEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> getBlocks = driver.findElements(By.cssSelector(".opblock.opblock-get"));
        boolean found = getBlocks.stream()
                .anyMatch(b -> b.getText().contains("/session-exams/result/"));
        System.out.println("[TC_SESSION_12] GET /session-exams/result found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(13)
    @DisplayName("TC_SESSION_13 - Session Exam module có >= 12 endpoints")
    void TC_SESSION_13_sessionExamModuleHasTwelveOrMoreEndpoints() throws InterruptedException {
        openAndExpand();
        List<WebElement> allPaths = driver.findElements(By.cssSelector(".opblock-summary-path"));
        long count = allPaths.stream()
                .filter(p -> p.getText().contains("/session-exams"))
                .count();
        System.out.println("[TC_SESSION_13] Số /session-exams endpoints: " + count);
        assertThat(count).as("Session Exam module phải có ít nhất 12 endpoints").isGreaterThanOrEqualTo(12);
    }

    @Test @Order(14)
    @DisplayName("TC_SESSION_14 - POST /api/session-exams/{id}/save (lưu câu trả lời) tồn tại")
    void TC_SESSION_14_saveAnswersEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> postBlocks = driver.findElements(By.cssSelector(".opblock.opblock-post"));
        boolean found = postBlocks.stream()
                .anyMatch(b -> b.getText().contains("session-exams") && b.getText().contains("/save"));
        System.out.println("[TC_SESSION_14] POST /session-exams/{id}/save found: " + found);
        assertThat(found).isTrue();
    }
}
