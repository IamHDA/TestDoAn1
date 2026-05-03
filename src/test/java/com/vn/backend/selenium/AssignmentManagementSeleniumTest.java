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
 * Selenium Test: Kiểm tra Assignment Management API qua Swagger UI.
 *
 * Endpoints (/api/assignments):
 *   POST   /create/{classroomId}              – Tạo bài tập
 *   GET    /detail/{id}                       – Lấy chi tiết bài tập
 *   PUT    /update/{id}                       – Cập nhật bài tập
 *   POST   /delete/{id}                       – Xóa bài tập (soft)
 *   POST   /list/{classroomId}                – Lấy danh sách bài tập của lớp
 *   PUT    /add-assignee/{id}                 – Thêm người được giao
 *   POST   /search/assignee                   – Tìm kiếm người được giao
 *   GET    /{id}/statistics                   – Thống kê bài tập
 *   GET    /{classroomId}/average-score-comparison  – So sánh điểm trung bình
 *   GET    /{classroomId}/improvement-trend   – Xu hướng cải thiện
 *
 * Test Cases: TC_ASSIGN_01 → TC_ASSIGN_10
 */
@DisplayName("Assignment Management API - Selenium Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AssignmentManagementSeleniumTest extends SeleniumBaseTest {

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
    @DisplayName("TC_ASSIGN_01 - Assignment Management tag group tồn tại trong Swagger")
    void TC_ASSIGN_01_assignmentTagGroupExists() {
        navigateTo(SWAGGER_PATH);
        WebDriverWait wait = createWait(LOAD_WAIT);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".opblock-tag")));
        List<WebElement> tagLinks = driver.findElements(By.cssSelector(".opblock-tag a"));
        boolean found = tagLinks.stream()
                .anyMatch(t -> t.getText().toLowerCase().contains("assignment"));
        System.out.println("[TC_ASSIGN_01] Assignment tag found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(2)
    @DisplayName("TC_ASSIGN_02 - POST /api/assignments/create/{classroomId} tồn tại")
    void TC_ASSIGN_02_createAssignmentEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> paths = driver.findElements(By.cssSelector(".opblock-summary-path"));
        boolean found = paths.stream().anyMatch(p -> p.getText().contains("/assignments/create/"));
        System.out.println("[TC_ASSIGN_02] /assignments/create found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(3)
    @DisplayName("TC_ASSIGN_03 - GET /api/assignments/detail/{id} tồn tại")
    void TC_ASSIGN_03_getAssignmentDetailEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> getBlocks = driver.findElements(By.cssSelector(".opblock.opblock-get"));
        boolean found = getBlocks.stream()
                .anyMatch(b -> b.getText().contains("/assignments/detail/"));
        System.out.println("[TC_ASSIGN_03] GET /assignments/detail found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(4)
    @DisplayName("TC_ASSIGN_04 - PUT /api/assignments/update/{id} tồn tại")
    void TC_ASSIGN_04_updateAssignmentEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> putBlocks = driver.findElements(By.cssSelector(".opblock.opblock-put"));
        boolean found = putBlocks.stream()
                .anyMatch(b -> b.getText().contains("/assignments/update/"));
        System.out.println("[TC_ASSIGN_04] PUT /assignments/update found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(5)
    @DisplayName("TC_ASSIGN_05 - POST /api/assignments/delete/{id} (soft delete) tồn tại")
    void TC_ASSIGN_05_softDeleteAssignmentEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> paths = driver.findElements(By.cssSelector(".opblock-summary-path"));
        boolean found = paths.stream().anyMatch(p -> p.getText().contains("/assignments/delete/"));
        System.out.println("[TC_ASSIGN_05] POST /assignments/delete found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(6)
    @DisplayName("TC_ASSIGN_06 - POST /api/assignments/list/{classroomId} tồn tại")
    void TC_ASSIGN_06_getAssignmentListEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> paths = driver.findElements(By.cssSelector(".opblock-summary-path"));
        boolean found = paths.stream().anyMatch(p -> p.getText().contains("/assignments/list/"));
        System.out.println("[TC_ASSIGN_06] POST /assignments/list found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(7)
    @DisplayName("TC_ASSIGN_07 - PUT /api/assignments/add-assignee/{id} tồn tại")
    void TC_ASSIGN_07_addAssigneeEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> putBlocks = driver.findElements(By.cssSelector(".opblock.opblock-put"));
        boolean found = putBlocks.stream()
                .anyMatch(b -> b.getText().contains("add-assignee"));
        System.out.println("[TC_ASSIGN_07] PUT add-assignee found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(8)
    @DisplayName("TC_ASSIGN_08 - GET /api/assignments/{id}/statistics tồn tại")
    void TC_ASSIGN_08_assignmentStatisticsEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> getBlocks = driver.findElements(By.cssSelector(".opblock.opblock-get"));
        boolean found = getBlocks.stream()
                .anyMatch(b -> b.getText().contains("/assignments/") && b.getText().contains("statistics"));
        System.out.println("[TC_ASSIGN_08] GET /assignments/{id}/statistics found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(9)
    @DisplayName("TC_ASSIGN_09 - GET /api/assignments/{id}/improvement-trend tồn tại")
    void TC_ASSIGN_09_improvementTrendEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> getBlocks = driver.findElements(By.cssSelector(".opblock.opblock-get"));
        boolean found = getBlocks.stream()
                .anyMatch(b -> b.getText().contains("improvement-trend"));
        System.out.println("[TC_ASSIGN_09] GET improvement-trend found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(10)
    @DisplayName("TC_ASSIGN_10 - Assignment module có đầy đủ >= 9 endpoints")
    void TC_ASSIGN_10_assignmentModuleHasNineOrMoreEndpoints() throws InterruptedException {
        openAndExpand();
        List<WebElement> allPaths = driver.findElements(By.cssSelector(".opblock-summary-path"));
        long count = allPaths.stream()
                .filter(p -> p.getText().contains("/assignments"))
                .count();
        System.out.println("[TC_ASSIGN_10] Số /assignments endpoints: " + count);
        assertThat(count).as("Assignment module phải có ít nhất 9 endpoints").isGreaterThanOrEqualTo(9);
    }
}
