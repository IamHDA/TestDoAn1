package com.vn.backend.selenium.core;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class SeleniumHelper {
    private final WebDriver driver;
    private final WebDriverWait wait;

    public SeleniumHelper(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public WebElement visible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public WebElement clickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    public void click(By locator) {
        WebElement element = clickable(locator);
        scrollIntoView(element);
        element.click();
    }

    public void clickButton(String text) {
        click(ByHelper.button(text));
    }

    public void clickText(String text) {
        click(ByHelper.linkOrButton(text));
    }

    public void type(By locator, String value) {
        WebElement element = visible(locator);
        scrollIntoView(element);
        element.clear();
        element.sendKeys(value);
    }

    public void typeById(String id, String value) {
        type(ByHelper.byId(id), value);
    }

    public void typeByPlaceholder(String placeholder, String value) {
        type(ByHelper.inputByPlaceholder(placeholder), value);
    }

    public void appendByPlaceholder(String placeholder, String value) {
        WebElement element = visible(ByHelper.inputByPlaceholder(placeholder));
        scrollIntoView(element);
        element.sendKeys(value);
    }

    public void clearById(String id) {
        WebElement element = visible(By.id(id));
        scrollIntoView(element);
        element.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        element.sendKeys(Keys.DELETE);
    }

    public void selectCustomOption(String placeholderText, String optionText) {
        click(By.xpath("//*[contains(normalize-space(.), '" + placeholderText + "')]") );
        click(ByHelper.exactText(optionText));
    }

    public void waitText(String text) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(ByHelper.text(text)));
    }

    public void waitSuccessToast() {
        wait.until(driver -> {
            String body = driver.findElement(By.tagName("body")).getText().toLowerCase();
            return body.contains("thành công") || body.contains("success");
        });
    }

    public void waitErrorOrValidation() {
        wait.until(driver -> {
            String body = driver.findElement(By.tagName("body")).getText().toLowerCase();
            return body.contains("không") || body.contains("lỗi") || body.contains("bắt buộc")
                    || body.contains("hợp lệ") || body.contains("vui lòng") || body.contains("invalid")
                    || body.contains("error");
        });
    }

    public void waitUrlContains(String path) {
        wait.until(ExpectedConditions.urlContains(path));
    }

    public void waitPageReady() {
        wait.withTimeout(Duration.ofSeconds(12)).until(driver ->
                "complete".equals(((JavascriptExecutor) driver).executeScript("return document.readyState"))
        );
    }

    public boolean hasText(String text) {
        try {
            return !driver.findElements(ByHelper.text(text)).isEmpty();
        } catch (NoSuchElementException ignored) {
            return false;
        }
    }

    public void clickFirstByText(String text) {
        List<WebElement> elements = driver.findElements(ByHelper.linkOrButton(text));
        if (elements.isEmpty()) {
            elements = driver.findElements(ByHelper.text(text));
        }
        if (elements.isEmpty()) {
            throw new NoSuchElementException("Cannot find text: " + text);
        }
        WebElement element = elements.get(0);
        scrollIntoView(element);
        element.click();
    }

    public void clickRowActionByText(String rowText, String actionText) {
        By locator = By.xpath("//*[self::tr or contains(@class,'card') or contains(@class,'rounded')][contains(normalize-space(.), '" + rowText + "')]//button[contains(normalize-space(.), '" + actionText + "')]");
        click(locator);
    }

    public void open(String path) {
        driver.get(TestConfig.BASE_URL + path);
        waitPageReady();
    }

    public void scrollIntoView(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
    }

    public void optionalClickText(String text) {
        try {
            clickText(text);
        } catch (TimeoutException | NoSuchElementException ignored) {
            // Optional UI element is not available in every seeded environment.
        }
    }
}
