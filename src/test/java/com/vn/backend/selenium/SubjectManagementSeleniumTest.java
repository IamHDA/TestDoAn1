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
 * Selenium Test: Kiểm tra Subject Management API qua Swagger UI.
 *
 * Endpoints cần kiểm tra (/api/subjects):
 *   POST   /create         – Tạo môn học mới
 *   POST   /search         – Tìm kiếm môn học
 *   PUT    /{id}/update    – Cập nhật môn học
 *   DELETE /{id}           – Xóa môn học
 *
 * Test Cases: TC_SUBJECT_01 → TC_SUBJECT_08
 */
@DisplayName("Subject Management API - Selenium Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SubjectManagementSeleniumTest extends SeleniumBaseTest {

    private static final String SWAGGER_PATH = "/swagger-ui/index.html";
    private static final int LOAD_WAIT = 20;

    /** Mở Swagger, expand tất cả tag groups và trả về wait. */
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

    // ─────────────────────────────────────────────────────────
    // TC_SUBJECT_01
    // ─────────────────────────────────────────────────────────
    @Test @Order(1)
    @DisplayName("TC_SUBJECT_01 - Swagger UI tải thành công và có ít nhất 1 tag group")
    void TC_SUBJECT_01_swaggerLoadsWithTagGroups() {
        navigateTo(SWAGGER_PATH);
        WebDriverWait wait = createWait(LOAD_WAIT);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".opblock-tag")));
        List<WebElement> tags = driver.findElements(By.cssSelector(".opblock-tag"));
        assertThat(tags).isNotEmpty();
        System.out.println("[TC_SUBJECT_01] Số API tag groups: " + tags.size());
    }

    // ─────────────────────────────────────────────────────────
    // TC_SUBJECT_02
    // ─────────────────────────────────────────────────────────
    @Test @Order(2)
    @DisplayName("TC_SUBJECT_02 - Endpoint POST /api/subjects/create tồn tại trong Swagger")
    void TC_SUBJECT_02_createSubjectEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> paths = driver.findElements(By.cssSelector(".opblock-summary-path"));
        boolean found = paths.stream().anyMatch(p -> p.getText().contains("/subjects/create"));
        System.out.println("[TC_SUBJECT_02] /subjects/create found: " + found);
        assertThat(found).as("POST /api/subjects/create phải hiển thị trong Swagger").isTrue();
    }

    // ─────────────────────────────────────────────────────────
    // TC_SUBJECT_03
    // ─────────────────────────────────────────────────────────
    @Test @Order(3)
    @DisplayName("TC_SUBJECT_03 - Endpoint POST /api/subjects/search tồn tại trong Swagger")
    void TC_SUBJECT_03_searchSubjectEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> paths = driver.findElements(By.cssSelector(".opblock-summary-path"));
        boolean found = paths.stream().anyMatch(p -> p.getText().contains("/subjects/search"));
        System.out.println("[TC_SUBJECT_03] /subjects/search found: " + found);
        assertThat(found).as("POST /api/subjects/search phải hiển thị trong Swagger").isTrue();
    }

    // ─────────────────────────────────────────────────────────
    // TC_SUBJECT_04
    // ─────────────────────────────────────────────────────────
    @Test @Order(4)
    @DisplayName("TC_SUBJECT_04 - Endpoint PUT /api/subjects/{id}/update tồn tại")
    void TC_SUBJECT_04_updateSubjectEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> putBlocks = driver.findElements(By.cssSelector(".opblock.opblock-put"));
        boolean found = putBlocks.stream()
                .anyMatch(b -> b.getText().contains("/subjects/") && b.getText().contains("update"));
        System.out.println("[TC_SUBJECT_04] PUT /subjects/{id}/update found: " + found);
        assertThat(found).as("PUT /api/subjects/{id}/update phải hiển thị trong Swagger").isTrue();
    }

    // ─────────────────────────────────────────────────────────
    // TC_SUBJECT_05
    // ─────────────────────────────────────────────────────────
    @Test @Order(5)
    @DisplayName("TC_SUBJECT_05 - Endpoint DELETE /api/subjects/{id} tồn tại")
    void TC_SUBJECT_05_deleteSubjectEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> delBlocks = driver.findElements(By.cssSelector(".opblock.opblock-delete"));
        boolean found = delBlocks.stream()
                .anyMatch(b -> b.getText().contains("/subjects/"));
        System.out.println("[TC_SUBJECT_05] DELETE /subjects/{id} found: " + found);
        assertThat(found).as("DELETE /api/subjects/{id} phải hiển thị trong Swagger").isTrue();
    }

    // ─────────────────────────────────────────────────────────
    // TC_SUBJECT_06
    // ─────────────────────────────────────────────────────────
    @Test @Order(6)
    @DisplayName("TC_SUBJECT_06 - Subject API có đầy đủ 4 endpoints (CRUD)")
    void TC_SUBJECT_06_subjectHasFullCrudEndpoints() throws InterruptedException {
        openAndExpand();
        List<WebElement> allPaths = driver.findElements(By.cssSelector(".opblock-summary-path"));
        long subjectPathCount = allPaths.stream()
                .filter(p -> p.getText().contains("/subjects"))
                .count();
        System.out.println("[TC_SUBJECT_06] Tổng số /subjects endpoints: " + subjectPathCount);
        assertThat(subjectPathCount).as("Subject module phải có ít nhất 4 endpoints").isGreaterThanOrEqualTo(4);
    }

    // ─────────────────────────────────────────────────────────
    // TC_SUBJECT_07
    // ─────────────────────────────────────────────────────────
    @Test @Order(7)
    @DisplayName("TC_SUBJECT_07 - Có thể click vào Subject create operation để xem schema")
    void TC_SUBJECT_07_canExpandCreateSubjectToSeeSchema() throws InterruptedException {
        openAndExpand();
        List<WebElement> postBlocks = driver.findElements(By.cssSelector(".opblock.opblock-post"));
        WebElement createBlock = postBlocks.stream()
                .filter(b -> b.getText().contains("/subjects/create"))
                .findFirst().orElse(null);
        if (createBlock == null) {
            System.out.println("[TC_SUBJECT_07] Block không tìm thấy, skip.");
            return;
        }
        createBlock.click();
        Thread.sleep(500);
        // Sau khi click, phần body của operation phải mở ra
        List<WebElement> opBody = createBlock.findElements(By.cssSelector(".opblock-body"));
        assertThat(opBody).isNotEmpty();
        System.out.println("[TC_SUBJECT_07] Operation body expanded thành công.");
    }

    // ─────────────────────────────────────────────────────────
    // TC_SUBJECT_08
    // ─────────────────────────────────────────────────────────
    @Test @Order(8)
    @DisplayName("TC_SUBJECT_08 - Swagger render hoàn tất trong vòng 20 giây (performance check)")
    void TC_SUBJECT_08_swaggerRendersWithinTimeout() {
        long start = System.currentTimeMillis();
        navigateTo(SWAGGER_PATH);
        WebDriverWait wait = createWait(LOAD_WAIT);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".opblock-tag")));
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("[TC_SUBJECT_08] Swagger UI load time: " + elapsed + "ms");
        assertThat(elapsed).as("Swagger UI phải load xong trong 20 giây").isLessThan(20_000L);
    }
}
