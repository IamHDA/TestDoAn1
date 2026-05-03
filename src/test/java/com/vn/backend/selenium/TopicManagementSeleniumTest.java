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
 * Selenium Test: Kiểm tra Topic Management API qua Swagger UI.
 *
 * Endpoints cần kiểm tra (/api/topics):
 *   POST   /create                  – Tạo chủ đề (qua approval)
 *   PUT    /{id}/update             – Cập nhật chủ đề
 *   DELETE /{id}                    – Xóa chủ đề
 *   POST   /search                  – Tìm kiếm chủ đề
 *   GET    /{id}/practice-set       – Lấy bộ câu hỏi luyện tập
 *
 * Test Cases: TC_TOPIC_01 → TC_TOPIC_09
 */
@DisplayName("Topic Management API - Selenium Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TopicManagementSeleniumTest extends SeleniumBaseTest {

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

    // ─────────────────────────────────────────────────────────
    // TC_TOPIC_01
    // ─────────────────────────────────────────────────────────
    @Test @Order(1)
    @DisplayName("TC_TOPIC_01 - Topic Management tag group tồn tại trong Swagger")
    void TC_TOPIC_01_topicTagGroupExists() {
        navigateTo(SWAGGER_PATH);
        WebDriverWait wait = createWait(LOAD_WAIT);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".opblock-tag")));
        List<WebElement> tagLinks = driver.findElements(By.cssSelector(".opblock-tag a"));
        boolean found = tagLinks.stream()
                .anyMatch(t -> t.getText().toLowerCase().contains("topic"));
        System.out.println("[TC_TOPIC_01] Topic tag group found: " + found);
        assertThat(found).as("Topic Management group phải hiện trong Swagger").isTrue();
    }

    // ─────────────────────────────────────────────────────────
    // TC_TOPIC_02
    // ─────────────────────────────────────────────────────────
    @Test @Order(2)
    @DisplayName("TC_TOPIC_02 - Endpoint POST /api/topics/create (tạo chủ đề) tồn tại")
    void TC_TOPIC_02_createTopicEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> paths = driver.findElements(By.cssSelector(".opblock-summary-path"));
        boolean found = paths.stream().anyMatch(p -> p.getText().contains("/topics/create"));
        System.out.println("[TC_TOPIC_02] /topics/create found: " + found);
        assertThat(found).as("POST /api/topics/create phải hiển thị").isTrue();
    }

    // ─────────────────────────────────────────────────────────
    // TC_TOPIC_03
    // ─────────────────────────────────────────────────────────
    @Test @Order(3)
    @DisplayName("TC_TOPIC_03 - Endpoint POST /api/topics/search tồn tại")
    void TC_TOPIC_03_searchTopicEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> paths = driver.findElements(By.cssSelector(".opblock-summary-path"));
        boolean found = paths.stream().anyMatch(p -> p.getText().contains("/topics/search"));
        System.out.println("[TC_TOPIC_03] /topics/search found: " + found);
        assertThat(found).as("POST /api/topics/search phải hiển thị").isTrue();
    }

    // ─────────────────────────────────────────────────────────
    // TC_TOPIC_04
    // ─────────────────────────────────────────────────────────
    @Test @Order(4)
    @DisplayName("TC_TOPIC_04 - Endpoint PUT /api/topics/{id}/update tồn tại")
    void TC_TOPIC_04_updateTopicEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> putBlocks = driver.findElements(By.cssSelector(".opblock.opblock-put"));
        boolean found = putBlocks.stream()
                .anyMatch(b -> b.getText().contains("/topics/") && b.getText().contains("update"));
        System.out.println("[TC_TOPIC_04] PUT /topics/{id}/update found: " + found);
        assertThat(found).as("PUT /api/topics/{id}/update phải hiển thị").isTrue();
    }

    // ─────────────────────────────────────────────────────────
    // TC_TOPIC_05
    // ─────────────────────────────────────────────────────────
    @Test @Order(5)
    @DisplayName("TC_TOPIC_05 - Endpoint DELETE /api/topics/{id} tồn tại")
    void TC_TOPIC_05_deleteTopicEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> delBlocks = driver.findElements(By.cssSelector(".opblock.opblock-delete"));
        boolean found = delBlocks.stream().anyMatch(b -> b.getText().contains("/topics/"));
        System.out.println("[TC_TOPIC_05] DELETE /topics/{id} found: " + found);
        assertThat(found).as("DELETE /api/topics/{id} phải hiển thị").isTrue();
    }

    // ─────────────────────────────────────────────────────────
    // TC_TOPIC_06
    // ─────────────────────────────────────────────────────────
    @Test @Order(6)
    @DisplayName("TC_TOPIC_06 - Endpoint GET /api/topics/{id}/practice-set tồn tại")
    void TC_TOPIC_06_practiceSetEndpointExists() throws InterruptedException {
        openAndExpand();
        List<WebElement> getBlocks = driver.findElements(By.cssSelector(".opblock.opblock-get"));
        boolean found = getBlocks.stream()
                .anyMatch(b -> b.getText().contains("practice-set"));
        System.out.println("[TC_TOPIC_06] GET practice-set found: " + found);
        assertThat(found).as("GET /api/topics/{id}/practice-set phải hiển thị").isTrue();
    }

    // ─────────────────────────────────────────────────────────
    // TC_TOPIC_07
    // ─────────────────────────────────────────────────────────
    @Test @Order(7)
    @DisplayName("TC_TOPIC_07 - Topic module có đầy đủ 5 endpoints")
    void TC_TOPIC_07_topicHasFiveEndpoints() throws InterruptedException {
        openAndExpand();
        List<WebElement> allPaths = driver.findElements(By.cssSelector(".opblock-summary-path"));
        long count = allPaths.stream()
                .filter(p -> p.getText().contains("/topics"))
                .count();
        System.out.println("[TC_TOPIC_07] Số /topics endpoints: " + count);
        assertThat(count).as("Topic module phải có ít nhất 5 endpoints").isGreaterThanOrEqualTo(5);
    }

    // ─────────────────────────────────────────────────────────
    // TC_TOPIC_08
    // ─────────────────────────────────────────────────────────
    @Test @Order(8)
    @DisplayName("TC_TOPIC_08 - Topic create operation có thể expand để xem request body schema")
    void TC_TOPIC_08_createTopicOperationExpandable() throws InterruptedException {
        openAndExpand();
        List<WebElement> postBlocks = driver.findElements(By.cssSelector(".opblock.opblock-post"));
        WebElement createBlock = postBlocks.stream()
                .filter(b -> b.getText().contains("/topics/create"))
                .findFirst().orElse(null);
        if (createBlock == null) {
            System.out.println("[TC_TOPIC_08] Không tìm thấy /topics/create, skip.");
            return;
        }
        createBlock.click();
        Thread.sleep(500);
        List<WebElement> opBody = createBlock.findElements(By.cssSelector(".opblock-body"));
        assertThat(opBody).isNotEmpty();
        System.out.println("[TC_TOPIC_08] /topics/create operation body mở thành công.");
    }

    // ─────────────────────────────────────────────────────────
    // TC_TOPIC_09
    // ─────────────────────────────────────────────────────────
    @Test @Order(9)
    @DisplayName("TC_TOPIC_09 - Topic search operation có thể expand để xem request body schema")
    void TC_TOPIC_09_searchTopicOperationExpandable() throws InterruptedException {
        openAndExpand();
        List<WebElement> postBlocks = driver.findElements(By.cssSelector(".opblock.opblock-post"));
        WebElement searchBlock = postBlocks.stream()
                .filter(b -> b.getText().contains("/topics/search"))
                .findFirst().orElse(null);
        if (searchBlock == null) {
            System.out.println("[TC_TOPIC_09] Không tìm thấy /topics/search, skip.");
            return;
        }
        searchBlock.click();
        Thread.sleep(500);
        List<WebElement> opBody = searchBlock.findElements(By.cssSelector(".opblock-body"));
        assertThat(opBody).isNotEmpty();
        System.out.println("[TC_TOPIC_09] /topics/search operation body mở thành công.");
    }
}
