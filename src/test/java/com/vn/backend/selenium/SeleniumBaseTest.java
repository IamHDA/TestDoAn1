package com.vn.backend.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Base class cho tất cả Selenium Tests.
 * <p>
 * - Khởi tạo WebDriver (Chrome) tự động qua WebDriverManager.
 * - Hỗ trợ chế độ Headless (mặc định) và GUI (tắt headless flag).
 * - Cung cấp các helper method thường dùng: getBaseUrl(), createWait().
 * </p>
 *
 * Cách chạy:
 * <pre>
 *   # Headless (CI/CD):
 *   mvn test -Dtest="*SeleniumTest" -Dselenium.headless=true
 *
 *   # GUI (local debug):
 *   mvn test -Dtest="*SeleniumTest" -Dselenium.headless=false
 * </pre>
 */
public abstract class SeleniumBaseTest {

    /**
     * WebDriver instance, được khởi tạo mới trước mỗi test method.
     */
    protected WebDriver driver;

    /**
     * Base URL của backend API (Swagger UI, Thymeleaf pages).
     * Mặc định: http://localhost:8080
     * Override bằng system property: -Dapp.base.url=http://...
     */
    protected static final String BASE_URL =
            System.getProperty("app.base.url", "http://localhost:8080");

    /**
     * Timeout mặc định cho WebDriverWait (giây).
     */
    protected static final int DEFAULT_WAIT_SECONDS = 15;

    /**
     * Flag headless: true = không mở browser (CI), false = mở browser (local debug).
     * Override bằng: -Dselenium.headless=false
     */
    private static final boolean HEADLESS =
            Boolean.parseBoolean(System.getProperty("selenium.headless", "true"));

    /**
     * Setup WebDriverManager một lần cho toàn bộ test class.
     * WebDriverManager sẽ tự tải ChromeDriver phù hợp với phiên bản Chrome đang cài.
     */
    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    /**
     * Khởi tạo WebDriver trước mỗi test method.
     * Cấu hình ChromeOptions với các flag tối ưu cho test.
     */
    @BeforeEach
    void setupDriver() {
        ChromeOptions options = buildChromeOptions();
        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
    }

    /**
     * Đóng WebDriver sau mỗi test method, dù test pass hay fail.
     */
    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    /**
     * Tạo ChromeOptions với các cài đặt phù hợp.
     */
    private ChromeOptions buildChromeOptions() {
        ChromeOptions options = new ChromeOptions();

        if (HEADLESS) {
            options.addArguments("--headless=new"); // headless mode mới (Chrome >= 112)
        }

        // Các flag tối ưu cho môi trường test (CI/CD và local)
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--disable-notifications");
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--lang=vi");

        return options;
    }

    /**
     * Helper: Tạo WebDriverWait với timeout mặc định.
     *
     * @return WebDriverWait instance
     */
    protected WebDriverWait createWait() {
        return new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_WAIT_SECONDS));
    }

    /**
     * Helper: Tạo WebDriverWait với timeout tùy chỉnh (giây).
     *
     * @param seconds số giây timeout
     * @return WebDriverWait instance
     */
    protected WebDriverWait createWait(int seconds) {
        return new WebDriverWait(driver, Duration.ofSeconds(seconds));
    }

    /**
     * Helper: Navigate đến một path tương đối từ BASE_URL.
     *
     * @param path ví dụ: "/swagger-ui/index.html"
     */
    protected void navigateTo(String path) {
        driver.get(BASE_URL + path);
    }
}
