package com.vn.backend.selenium.core;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public abstract class BaseSeleniumTest {
    protected WebDriver driver;
    protected WebDriverWait wait;
    protected SeleniumHelper ui;

    @BeforeEach
    void setUpBrowser() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();

        if (Boolean.parseBoolean(System.getProperty("headless", "false"))) {
            options.addArguments("--headless=new");
        }

        options.addArguments("--start-maximized");
        options.addArguments("--disable-notifications");
        options.addArguments("--remote-allow-origins=*");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(Long.parseLong(System.getProperty("wait.seconds", "12"))));
        ui = new SeleniumHelper(driver, wait);
    }

    @AfterEach
    void tearDownBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }

    protected void loginAsAdmin() {
        login(TestConfig.ADMIN_USERNAME, TestConfig.ADMIN_PASSWORD);
        ui.waitUrlContains("admin");
    }

    protected void loginAsTeacher() {
        login(TestConfig.TEACHER_USERNAME, TestConfig.TEACHER_PASSWORD);
    }

    protected void loginAsStudent() {
        login(TestConfig.STUDENT_USERNAME, TestConfig.STUDENT_PASSWORD);
    }

    protected void login(String username, String password) {
        driver.get(TestConfig.BASE_URL + "/login");
        ui.typeByPlaceholder("Tên đăng nhập", username);
        ui.type(ByHelper.passwordInput(), password);
        ui.clickButton("Đăng nhập");
        ui.waitPageReady();
    }
}
