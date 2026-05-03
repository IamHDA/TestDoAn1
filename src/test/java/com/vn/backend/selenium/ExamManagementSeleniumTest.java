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
 * Selenium Test: Kiểm tra Exam Management API qua Swagger UI.
 *
 * Endpoints (/api/exams):
 *   POST   /create                      – Tạo đề thi
 *   POST   /search                      – Tìm kiếm đề thi
 *   GET    /{id}                        – Lấy chi tiết đề thi
 *   PUT    /{id}                        – Cập nhật đề thi
 *   DELETE /{id}                        – Xóa đề thi
 *   POST   /{id}/questions/create       – Thêm câu hỏi vào đề thi
 *   POST   /questions/search            – Tìm câu hỏi trong đề thi
 *   POST   /{id}/duplicate              – Nhân bản đề thi
 *   GET    /{id}/statistic              – Lấy thống kê đề thi
 *   POST   /available-questions/search  – Tìm câu hỏi có thể thêm vào đề thi
 *
 * Test Cases: TC_EXAM_01 → TC_EXAM_10
 */
@DisplayName("Exam Management API - Selenium Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExamManagementSeleniumTest extends SeleniumBaseTest {

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
    @DisplayName("TC_EXAM_01 - POST /api/exams/create endpoint tồn tại")
    void TC_EXAM_01_createExamEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> paths = driver.findElements(By.cssSelector(".opblock-summary-path"));
        boolean found = paths.stream().anyMatch(p -> p.getText().contains("/exams/create"));
        System.out.println("[TC_EXAM_01] /exams/create found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(2)
    @DisplayName("TC_EXAM_02 - POST /api/exams/search endpoint tồn tại")
    void TC_EXAM_02_searchExamEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> paths = driver.findElements(By.cssSelector(".opblock-summary-path"));
        boolean found = paths.stream().anyMatch(p -> p.getText().contains("/exams/search"));
        System.out.println("[TC_EXAM_02] /exams/search found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(3)
    @DisplayName("TC_EXAM_03 - GET /api/exams/{id} (chi tiết đề thi) tồn tại")
    void TC_EXAM_03_getExamDetailEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> getBlocks = driver.findElements(By.cssSelector(".opblock.opblock-get"));
        // Exam detail: GET /api/exams/{examId} — path chứa "exams/" nhưng không phải statistic hay template
        boolean found = getBlocks.stream()
                .anyMatch(b -> {
                    String text = b.getText();
                    return text.contains("/exams/") && !text.contains("statistic")
                            && !text.contains("template") && !text.contains("questions");
                });
        System.out.println("[TC_EXAM_03] GET /exams/{id} found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(4)
    @DisplayName("TC_EXAM_04 - PUT /api/exams/{id} (cập nhật đề thi) tồn tại")
    void TC_EXAM_04_updateExamEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> putBlocks = driver.findElements(By.cssSelector(".opblock.opblock-put"));
        boolean found = putBlocks.stream().anyMatch(b -> b.getText().contains("/exams/"));
        System.out.println("[TC_EXAM_04] PUT /exams/{id} found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(5)
    @DisplayName("TC_EXAM_05 - DELETE /api/exams/{id} (xóa đề thi) tồn tại")
    void TC_EXAM_05_deleteExamEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> delBlocks = driver.findElements(By.cssSelector(".opblock.opblock-delete"));
        boolean found = delBlocks.stream().anyMatch(b -> b.getText().contains("/exams/"));
        System.out.println("[TC_EXAM_05] DELETE /exams/{id} found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(6)
    @DisplayName("TC_EXAM_06 - POST /api/exams/{id}/questions/create (thêm câu hỏi) tồn tại")
    void TC_EXAM_06_addQuestionsToExamEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> postBlocks = driver.findElements(By.cssSelector(".opblock.opblock-post"));
        boolean found = postBlocks.stream()
                .anyMatch(b -> b.getText().contains("/exams/") && b.getText().contains("questions/create"));
        System.out.println("[TC_EXAM_06] POST /exams/{id}/questions/create found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(7)
    @DisplayName("TC_EXAM_07 - POST /api/exams/{id}/duplicate (nhân bản đề) tồn tại")
    void TC_EXAM_07_duplicateExamEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> postBlocks = driver.findElements(By.cssSelector(".opblock.opblock-post"));
        boolean found = postBlocks.stream()
                .anyMatch(b -> b.getText().contains("duplicate"));
        System.out.println("[TC_EXAM_07] POST /exams/{id}/duplicate found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(8)
    @DisplayName("TC_EXAM_08 - GET /api/exams/{id}/statistic (thống kê đề thi) tồn tại")
    void TC_EXAM_08_examStatisticEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> getBlocks = driver.findElements(By.cssSelector(".opblock.opblock-get"));
        boolean found = getBlocks.stream()
                .anyMatch(b -> b.getText().contains("/exams/") && b.getText().contains("statistic"));
        System.out.println("[TC_EXAM_08] GET /exams/{id}/statistic found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(9)
    @DisplayName("TC_EXAM_09 - POST /api/exams/available-questions/search tồn tại")
    void TC_EXAM_09_availableQuestionsSearchEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> paths = driver.findElements(By.cssSelector(".opblock-summary-path"));
        boolean found = paths.stream().anyMatch(p -> p.getText().contains("available-questions"));
        System.out.println("[TC_EXAM_09] available-questions/search found: " + found);
        assertThat(found).isTrue();
    }

    @Test @Order(10)
    @DisplayName("TC_EXAM_10 - Exam module có đầy đủ >= 9 endpoints")
    void TC_EXAM_10_examModuleHasNineOrMoreEndpoints() throws InterruptedException {
        openAndExpand();
        List<WebElement> allPaths = driver.findElements(By.cssSelector(".opblock-summary-path"));
        long count = allPaths.stream()
                .filter(p -> p.getText().contains("/exams"))
                .count();
        System.out.println("[TC_EXAM_10] Số /exams endpoints: " + count);
        assertThat(count).as("Exam module phải có ít nhất 9 endpoints").isGreaterThanOrEqualTo(9);
    }
}
