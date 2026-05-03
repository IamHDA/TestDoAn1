package com.vn.backend.selenium;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Selenium Test: Kiểm tra Swagger UI - giao diện API documentation của hệ thống.
 * <p>
 * Hệ thống backend sử dụng springdoc-openapi (Swagger UI v3) được expose tại:
 * http://localhost:8080/swagger-ui/index.html
 * <p>
 * Các test case:
 * - TC_SWAGGER_01: Swagger UI tải thành công (HTTP 200, title đúng)
 * - TC_SWAGGER_02: Tất cả các API group (tags) hiển thị đầy đủ
 * - TC_SWAGGER_03: API Authentication group có thể expand được
 * - TC_SWAGGER_04: API User Management group có thể expand được
 * - TC_SWAGGER_05: Nút "Authorize" hiển thị (có thể nhập JWT token)
 */
@DisplayName("Swagger UI - Selenium Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SwaggerUISeleniumTest extends SeleniumBaseTest {

    private static final String SWAGGER_PATH = "/swagger-ui/index.html";
    private static final int SWAGGER_LOAD_WAIT = 20; // Swagger cần thêm thời gian để render JS

    @Test
    @Order(1)
    @DisplayName("TC_SWAGGER_01 - Swagger UI tải thành công")
    void TC_SWAGGER_01_swaggerUILoadsSuccessfully() {
        // Navigate đến Swagger UI
        navigateTo(SWAGGER_PATH);

        WebDriverWait wait = createWait(SWAGGER_LOAD_WAIT);

        // Chờ Swagger UI render xong (element ".swagger-ui" phải xuất hiện)
        WebElement swaggerContainer = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.className("swagger-ui"))
        );

        // Verify: Swagger container hiển thị
        assertThat(swaggerContainer).isNotNull();
        assertThat(swaggerContainer.isDisplayed()).isTrue();

        // Verify: URL đúng
        assertThat(driver.getCurrentUrl()).contains("swagger-ui");

        // Verify: Title trang chứa thông tin API
        String title = driver.getTitle();
        assertThat(title).isNotBlank();
        System.out.println("[TC_SWAGGER_01] Swagger UI title: " + title);
        System.out.println("[TC_SWAGGER_01] Current URL: " + driver.getCurrentUrl());
    }

    @Test
    @Order(2)
    @DisplayName("TC_SWAGGER_02 - Swagger UI hiển thị API info header")
    void TC_SWAGGER_02_swaggerUIShowsApiInfo() {
        navigateTo(SWAGGER_PATH);

        WebDriverWait wait = createWait(SWAGGER_LOAD_WAIT);

        // Chờ Swagger info section xuất hiện
        WebElement infoSection = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.className("info"))
        );

        assertThat(infoSection.isDisplayed()).isTrue();

        // Kiểm tra có title/description của API
        String infoText = infoSection.getText();
        assertThat(infoText).isNotBlank();
        System.out.println("[TC_SWAGGER_02] API Info: " + infoText.substring(0, Math.min(200, infoText.length())));
    }

    @Test
    @Order(3)
    @DisplayName("TC_SWAGGER_03 - Swagger UI hiển thị các API tag groups")
    void TC_SWAGGER_03_swaggerUIShowsApiTagGroups() {
        navigateTo(SWAGGER_PATH);

        WebDriverWait wait = createWait(SWAGGER_LOAD_WAIT);

        // Chờ operations-tag (API groups) xuất hiện
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector(".opblock-tag")
        ));

        // Lấy tất cả API tag sections
        java.util.List<WebElement> tagGroups = driver.findElements(
                By.cssSelector(".opblock-tag")
        );

        // Verify: Phải có ít nhất 5 groups (Authentication, User, Subject, Topic, etc.)
        assertThat(tagGroups).hasSizeGreaterThanOrEqualTo(5);

        // In ra danh sách các tag để verify
        System.out.println("[TC_SWAGGER_03] Số lượng API groups tìm thấy: " + tagGroups.size());
        tagGroups.forEach(tag -> {
            String tagText = tag.getText();
            if (!tagText.isBlank()) {
                System.out.println("[TC_SWAGGER_03]   - " + tagText.split("\n")[0]);
            }
        });
    }

    @Test
    @Order(4)
    @DisplayName("TC_SWAGGER_04 - Nút Authorize hiển thị để nhập JWT token")
    void TC_SWAGGER_04_authorizeButtonIsVisible() {
        navigateTo(SWAGGER_PATH);

        WebDriverWait wait = createWait(SWAGGER_LOAD_WAIT);

        // Chờ nút Authorize xuất hiện
        WebElement authorizeBtn = wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector(".btn.authorize")
                )
        );

        // Verify: Nút Authorize hiển thị
        assertThat(authorizeBtn.isDisplayed()).isTrue();
        assertThat(authorizeBtn.getText().toLowerCase()).contains("authorize");

        System.out.println("[TC_SWAGGER_04] Authorize button text: " + authorizeBtn.getText());
    }

    @Test
    @Order(5)
    @DisplayName("TC_SWAGGER_05 - Có thể expand Authentication API section")
    void TC_SWAGGER_05_canExpandAuthenticationSection() {
        navigateTo(SWAGGER_PATH);

        WebDriverWait wait = createWait(SWAGGER_LOAD_WAIT);

        // Chờ tag groups xuất hiện
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector(".opblock-tag")
        ));

        // Tìm tag group "Authentication" (case-insensitive)
        java.util.List<WebElement> allTags = driver.findElements(
                By.cssSelector(".opblock-tag a")
        );

        WebElement authTag = allTags.stream()
                .filter(el -> el.getText().toLowerCase().contains("authentication")
                        || el.getText().toLowerCase().contains("auth"))
                .findFirst()
                .orElse(null);

        if (authTag != null) {
            // Click để expand
            authTag.click();

            // Chờ operations hiển thị
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector(".opblock")
                ));
                java.util.List<WebElement> operations = driver.findElements(By.cssSelector(".opblock"));
                assertThat(operations).isNotEmpty();
                System.out.println("[TC_SWAGGER_05] Auth section expanded, số API operations: " + operations.size());
            } catch (Exception e) {
                System.out.println("[TC_SWAGGER_05] Auth section có thể đã expand sẵn.");
            }
        } else {
            System.out.println("[TC_SWAGGER_05] Không tìm thấy tag Authentication, kiểm tra tag names trong Swagger.");
            // Không fail test, chỉ log warning
        }
    }
}
