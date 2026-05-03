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
 * Selenium Test: Kiểm tra Question Management API qua Swagger UI.
 *
 * Endpoints (/api/questions):
 *   POST   /create                – Tạo câu hỏi
 *   POST   /bulk-create           – Tạo nhiều câu hỏi cùng lúc
 *   POST   /search                – Tìm kiếm câu hỏi
 *   GET    /detail/{id}           – Lấy chi tiết câu hỏi
 *   PUT    /update/{id}           – Cập nhật câu hỏi
 *   DELETE /delete/{id}           – Xóa câu hỏi (soft delete)
 *   POST   /import-excel          – Import câu hỏi từ Excel
 *   POST   /export-excel          – Xuất câu hỏi ra Excel
 *   GET    /import-template       – Download template Excel
 *   POST   /approval-question     – Tạo câu hỏi ôn tập (approval)
 *
 * Test Cases: TC_QUESTION_01 → TC_QUESTION_10
 */
@DisplayName("Question Management API - Selenium Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QuestionManagementSeleniumTest extends SeleniumBaseTest {

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
    @DisplayName("TC_QUESTION_01 - POST /api/questions/create endpoint tồn tại")
    void TC_QUESTION_01_createQuestionEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> paths = driver.findElements(By.cssSelector(".opblock-summary-path"));
        boolean found = paths.stream().anyMatch(p -> p.getText().contains("/questions/create"));
        System.out.println("[TC_QUESTION_01] /questions/create found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(2)
    @DisplayName("TC_QUESTION_02 - POST /api/questions/bulk-create endpoint tồn tại")
    void TC_QUESTION_02_bulkCreateQuestionEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> paths = driver.findElements(By.cssSelector(".opblock-summary-path"));
        boolean found = paths.stream().anyMatch(p -> p.getText().contains("bulk-create"));
        System.out.println("[TC_QUESTION_02] /questions/bulk-create found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(3)
    @DisplayName("TC_QUESTION_03 - POST /api/questions/search endpoint tồn tại")
    void TC_QUESTION_03_searchQuestionEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> paths = driver.findElements(By.cssSelector(".opblock-summary-path"));
        boolean found = paths.stream().anyMatch(p -> p.getText().contains("/questions/search"));
        System.out.println("[TC_QUESTION_03] /questions/search found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(4)
    @DisplayName("TC_QUESTION_04 - GET /api/questions/detail/{id} endpoint tồn tại")
    void TC_QUESTION_04_getQuestionDetailEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> getBlocks = driver.findElements(By.cssSelector(".opblock.opblock-get"));
        boolean found = getBlocks.stream()
                .anyMatch(b -> b.getText().contains("/questions/detail/"));
        System.out.println("[TC_QUESTION_04] GET /questions/detail/{id} found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(5)
    @DisplayName("TC_QUESTION_05 - PUT /api/questions/update/{id} endpoint tồn tại")
    void TC_QUESTION_05_updateQuestionEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> putBlocks = driver.findElements(By.cssSelector(".opblock.opblock-put"));
        boolean found = putBlocks.stream()
                .anyMatch(b -> b.getText().contains("/questions/update/"));
        System.out.println("[TC_QUESTION_05] PUT /questions/update/{id} found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(6)
    @DisplayName("TC_QUESTION_06 - DELETE /api/questions/delete/{id} (soft delete) tồn tại")
    void TC_QUESTION_06_softDeleteQuestionEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> delBlocks = driver.findElements(By.cssSelector(".opblock.opblock-delete"));
        boolean found = delBlocks.stream()
                .anyMatch(b -> b.getText().contains("/questions/delete/"));
        System.out.println("[TC_QUESTION_06] DELETE /questions/delete/{id} found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(7)
    @DisplayName("TC_QUESTION_07 - POST /api/questions/import-excel tồn tại")
    void TC_QUESTION_07_importExcelEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> paths = driver.findElements(By.cssSelector(".opblock-summary-path"));
        boolean found = paths.stream().anyMatch(p -> p.getText().contains("import-excel"));
        System.out.println("[TC_QUESTION_07] /questions/import-excel found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(8)
    @DisplayName("TC_QUESTION_08 - POST /api/questions/export-excel tồn tại")
    void TC_QUESTION_08_exportExcelEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> paths = driver.findElements(By.cssSelector(".opblock-summary-path"));
        boolean found = paths.stream().anyMatch(p -> p.getText().contains("export-excel"));
        System.out.println("[TC_QUESTION_08] /questions/export-excel found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(9)
    @DisplayName("TC_QUESTION_09 - GET /api/questions/import-template (download template) tồn tại")
    void TC_QUESTION_09_importTemplateEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> getBlocks = driver.findElements(By.cssSelector(".opblock.opblock-get"));
        boolean found = getBlocks.stream()
                .anyMatch(b -> b.getText().contains("import-template") && b.getText().contains("question"));
        System.out.println("[TC_QUESTION_09] GET /questions/import-template found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(10)
    @DisplayName("TC_QUESTION_10 - Question module có đầy đủ >= 9 endpoints")
    void TC_QUESTION_10_questionModuleHasNineOrMoreEndpoints() throws InterruptedException {
        openAndExpand();
        List<WebElement> allPaths = driver.findElements(By.cssSelector(".opblock-summary-path"));
        long count = allPaths.stream()
                .filter(p -> p.getText().contains("/questions"))
                .count();
        System.out.println("[TC_QUESTION_10] Số /questions endpoints: " + count);
        assertThat(count).as("Question module phải có ít nhất 9 endpoints").isGreaterThanOrEqualTo(9);
    }
}
