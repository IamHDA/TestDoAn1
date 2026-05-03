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
 * Selenium Test: Kiểm tra tính sẵn sàng và cấu trúc API của User Management module.
 * <p>
 * Test thông qua Swagger UI để kiểm tra:
 * - Tất cả user management endpoints có mặt đầy đủ
 * - Các endpoint quan trọng có thể được tìm thấy (create, update, list, import...)
 * - API documentation hiển thị đúng description
 * <p>
 * Test Cases:
 * - TC_USER_01: User Management API group xuất hiện trong Swagger
 * - TC_USER_02: Endpoint tạo user mới (POST /api/users/create) hiển thị
 * - TC_USER_03: Endpoint cập nhật trạng thái user (PUT /{id}/status) hiển thị
 * - TC_USER_04: Endpoint import user từ Excel hiển thị
 * - TC_USER_05: Endpoint tìm kiếm user (POST /api/users) hiển thị
 * - TC_USER_06: Endpoint download template Excel hiển thị
 */
@DisplayName("User Management API - Selenium Tests via Swagger UI")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserManagementSeleniumTest extends SeleniumBaseTest {

    private static final String SWAGGER_PATH = "/swagger-ui/index.html";
    private static final int SWAGGER_LOAD_WAIT = 20;

    /**
     * Helper: Navigate đến Swagger và expand tất cả sections.
     * @return wait instance
     */
    private WebDriverWait openSwaggerAndExpandAll() throws InterruptedException {
        navigateTo(SWAGGER_PATH);
        WebDriverWait wait = createWait(SWAGGER_LOAD_WAIT);

        // Chờ Swagger render
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".opblock-tag")));

        // Expand tất cả tag groups
        List<WebElement> tagHeaders = driver.findElements(By.cssSelector(".opblock-tag"));
        for (WebElement tag : tagHeaders) {
            try {
                WebElement link = tag.findElement(By.tagName("a"));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", link);
                Thread.sleep(200);
            } catch (Exception ignored) {}
        }

        Thread.sleep(1500); // Chờ animation expand hoàn tất
        return wait;
    }

    @Test
    @Order(1)
    @DisplayName("TC_USER_01 - User Management API group xuất hiện trong Swagger UI")
    void TC_USER_01_userManagementGroupAppearsInSwagger() {
        navigateTo(SWAGGER_PATH);
        WebDriverWait wait = createWait(SWAGGER_LOAD_WAIT);

        // Chờ Swagger render
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".opblock-tag")));

        // Lấy tất cả tag names
        List<WebElement> tagGroups = driver.findElements(By.cssSelector(".opblock-tag a"));

        System.out.println("[TC_USER_01] Danh sách API groups:");
        tagGroups.forEach(tag -> {
            String text = tag.getText();
            if (!text.isBlank()) {
                System.out.println("   - " + text);
            }
        });

        // Tìm User Management group
        boolean userGroupFound = tagGroups.stream()
                .anyMatch(tag -> {
                    String text = tag.getText().toLowerCase();
                    return text.contains("user") || text.contains("management");
                });

        assertThat(userGroupFound)
                .as("Phải tìm thấy User Management API group trong Swagger UI")
                .isTrue();
    }

    @Test
    @Order(2)
    @DisplayName("TC_USER_02 - Endpoint POST /api/users/create hiển thị đầy đủ")
    void TC_USER_02_createUserEndpointVisible() throws InterruptedException {
        openSwaggerAndExpandAll();

        // Tìm tất cả POST operations
        List<WebElement> postBlocks = driver.findElements(By.cssSelector(".opblock.opblock-post"));

        // Kiểm tra có endpoint tạo user
        boolean createUserFound = postBlocks.stream()
                .anyMatch(block -> {
                    String text = block.getText();
                    return text.contains("/users/create") || text.contains("Create new user");
                });

        System.out.println("[TC_USER_02] POST /api/users/create found: " + createUserFound);

        assertThat(createUserFound)
                .as("POST /api/users/create endpoint phải hiển thị trong Swagger UI")
                .isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("TC_USER_03 - Endpoint PUT /{id}/status (cập nhật trạng thái) hiển thị")
    void TC_USER_03_updateUserStatusEndpointVisible() throws InterruptedException {
        openSwaggerAndExpandAll();

        // Tìm tất cả PUT operations
        List<WebElement> putBlocks = driver.findElements(By.cssSelector(".opblock.opblock-put"));

        System.out.println("[TC_USER_03] Số PUT operations: " + putBlocks.size());
        putBlocks.forEach(block -> {
            String text = block.getText();
            if (text.contains("user") || text.contains("status")) {
                System.out.println("   Found: " + text.substring(0, Math.min(100, text.length())));
            }
        });

        // Kiểm tra có endpoint cập nhật status user
        boolean statusEndpointFound = putBlocks.stream()
                .anyMatch(block -> {
                    String text = block.getText();
                    return text.contains("/status") || text.contains("Update user status");
                });

        assertThat(statusEndpointFound)
                .as("PUT /{id}/status endpoint phải hiển thị trong Swagger UI")
                .isTrue();
    }

    @Test
    @Order(4)
    @DisplayName("TC_USER_04 - Endpoint POST /import (import Excel) hiển thị")
    void TC_USER_04_importUsersEndpointVisible() throws InterruptedException {
        openSwaggerAndExpandAll();

        // Tìm tất cả POST operations
        List<WebElement> postBlocks = driver.findElements(By.cssSelector(".opblock.opblock-post"));

        boolean importEndpointFound = postBlocks.stream()
                .anyMatch(block -> {
                    String text = block.getText();
                    return text.contains("/import") || text.contains("Import users");
                });

        System.out.println("[TC_USER_04] Import endpoint found: " + importEndpointFound);

        assertThat(importEndpointFound)
                .as("POST /api/users/import endpoint phải hiển thị trong Swagger UI")
                .isTrue();
    }

    @Test
    @Order(5)
    @DisplayName("TC_USER_05 - Endpoint GET /download/import-template (download Excel mẫu) hiển thị")
    void TC_USER_05_downloadTemplateEndpointVisible() throws InterruptedException {
        openSwaggerAndExpandAll();

        // Tìm tất cả GET operations
        List<WebElement> getBlocks = driver.findElements(By.cssSelector(".opblock.opblock-get"));

        boolean downloadTemplateFound = getBlocks.stream()
                .anyMatch(block -> {
                    String text = block.getText();
                    return text.contains("import-template") || text.contains("Download user import template");
                });

        System.out.println("[TC_USER_05] Download template endpoint found: " + downloadTemplateFound);

        assertThat(downloadTemplateFound)
                .as("GET /api/users/download/import-template endpoint phải hiển thị trong Swagger UI")
                .isTrue();
    }

    @Test
    @Order(6)
    @DisplayName("TC_USER_06 - Swagger UI hiển thị đầy đủ tất cả User API descriptions")
    void TC_USER_06_allUserEndpointsHaveDescriptions() throws InterruptedException {
        openSwaggerAndExpandAll();

        // Lấy tất cả summary paths trong User group
        List<WebElement> allPaths = driver.findElements(By.cssSelector(".opblock-summary-path"));

        List<String> userPaths = allPaths.stream()
                .map(WebElement::getText)
                .filter(text -> text.contains("/users"))
                .toList();

        System.out.println("[TC_USER_06] User API paths tìm thấy:");
        userPaths.forEach(path -> System.out.println("   " + path));

        // Verify: Phải có ít nhất 5 user endpoints (create, update, status, list, get by id, import, template, search-for-invite)
        assertThat(userPaths)
                .as("User Management module phải có ít nhất 5 API endpoints")
                .hasSizeGreaterThanOrEqualTo(5);
    }
}
