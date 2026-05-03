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
 * Selenium Test: Kiểm tra Classroom Management API qua Swagger UI.
 *
 * Endpoints (/api/classrooms):
 *   POST   /create                         – Tạo lớp học
 *   POST   /search                         – Tìm kiếm lớp học
 *   GET    /detail/{id}                    – Lấy chi tiết lớp
 *   GET    /{id}/header                    – Lấy header lớp
 *   PUT    /update/{id}                    – Cập nhật lớp
 *   PUT    /reset-class-code/{id}          – Reset mã lớp
 *   GET    /setting/{id}/detail            – Lấy cài đặt lớp
 *   PUT    /setting/{id}/update            – Cập nhật cài đặt lớp
 *   POST   /member/search                  – Tìm thành viên
 *   PUT    /member/{id}/update-status      – Cập nhật trạng thái thành viên
 *
 * Test Cases: TC_CLASS_01 → TC_CLASS_10
 */
@DisplayName("Classroom Management API - Selenium Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClassroomManagementSeleniumTest extends SeleniumBaseTest {

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
    @DisplayName("TC_CLASS_01 - POST /api/classrooms/create endpoint tồn tại")
    void TC_CLASS_01_createClassroomEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> paths = driver.findElements(By.cssSelector(".opblock-summary-path"));
        boolean found = paths.stream().anyMatch(p -> p.getText().contains("/classrooms/create"));
        System.out.println("[TC_CLASS_01] /classrooms/create found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(2)
    @DisplayName("TC_CLASS_02 - POST /api/classrooms/search endpoint tồn tại")
    void TC_CLASS_02_searchClassroomEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> paths = driver.findElements(By.cssSelector(".opblock-summary-path"));
        boolean found = paths.stream().anyMatch(p -> p.getText().contains("/classrooms/search"));
        System.out.println("[TC_CLASS_02] /classrooms/search found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(3)
    @DisplayName("TC_CLASS_03 - GET /api/classrooms/detail/{id} endpoint tồn tại")
    void TC_CLASS_03_getDetailEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> getBlocks = driver.findElements(By.cssSelector(".opblock.opblock-get"));
        boolean found = getBlocks.stream().anyMatch(b -> b.getText().contains("/classrooms/detail/"));
        System.out.println("[TC_CLASS_03] GET /classrooms/detail found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(4)
    @DisplayName("TC_CLASS_04 - GET /api/classrooms/{id}/header endpoint tồn tại")
    void TC_CLASS_04_getClassroomHeaderEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> getBlocks = driver.findElements(By.cssSelector(".opblock.opblock-get"));
        boolean found = getBlocks.stream().anyMatch(b -> b.getText().contains("/header"));
        System.out.println("[TC_CLASS_04] GET /classrooms/{id}/header found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(5)
    @DisplayName("TC_CLASS_05 - PUT /api/classrooms/update/{id} endpoint tồn tại")
    void TC_CLASS_05_updateClassroomEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> putBlocks = driver.findElements(By.cssSelector(".opblock.opblock-put"));
        boolean found = putBlocks.stream()
                .anyMatch(b -> b.getText().contains("/classrooms/update/"));
        System.out.println("[TC_CLASS_05] PUT /classrooms/update found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(6)
    @DisplayName("TC_CLASS_06 - PUT /api/classrooms/reset-class-code/{id} endpoint tồn tại")
    void TC_CLASS_06_resetClassCodeEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> putBlocks = driver.findElements(By.cssSelector(".opblock.opblock-put"));
        boolean found = putBlocks.stream().anyMatch(b -> b.getText().contains("reset-class-code"));
        System.out.println("[TC_CLASS_06] PUT reset-class-code found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(7)
    @DisplayName("TC_CLASS_07 - GET /api/classrooms/setting/{id}/detail endpoint tồn tại")
    void TC_CLASS_07_getClassroomSettingDetailExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> getBlocks = driver.findElements(By.cssSelector(".opblock.opblock-get"));
        boolean found = getBlocks.stream()
                .anyMatch(b -> b.getText().contains("setting/") && b.getText().contains("detail"));
        System.out.println("[TC_CLASS_07] GET setting/detail found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(8)
    @DisplayName("TC_CLASS_08 - POST /api/classrooms/member/search endpoint tồn tại")
    void TC_CLASS_08_searchClassMemberEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> paths = driver.findElements(By.cssSelector(".opblock-summary-path"));
        boolean found = paths.stream().anyMatch(p -> p.getText().contains("/classrooms/member/search"));
        System.out.println("[TC_CLASS_08] /classrooms/member/search found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(9)
    @DisplayName("TC_CLASS_09 - PUT /api/classrooms/member/{id}/update-status endpoint tồn tại")
    void TC_CLASS_09_updateClassMemberStatusEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> putBlocks = driver.findElements(By.cssSelector(".opblock.opblock-put"));
        boolean found = putBlocks.stream()
                .anyMatch(b -> b.getText().contains("member/") && b.getText().contains("update-status"));
        System.out.println("[TC_CLASS_09] PUT member/update-status found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(10)
    @DisplayName("TC_CLASS_10 - Classroom module có đầy đủ >= 9 endpoints")
    void TC_CLASS_10_classroomHasNineOrMoreEndpoints() throws InterruptedException {
        openAndExpand();
        List<WebElement> allPaths = driver.findElements(By.cssSelector(".opblock-summary-path"));
        long count = allPaths.stream()
                .filter(p -> p.getText().contains("/classrooms"))
                .count();
        System.out.println("[TC_CLASS_10] Số /classrooms endpoints: " + count);
        assertThat(count).as("Classroom module phải có ít nhất 9 endpoints").isGreaterThanOrEqualTo(9);
    }
}
