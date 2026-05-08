package com.vn.backend.selenium.core;

import org.openqa.selenium.By;

public final class ByHelper {
    private ByHelper() {}

    public static By passwordInput() {
        return By.cssSelector("input[type='password']");
    }

    public static By byId(String id) {
        return By.id(id);
    }

    public static By inputByPlaceholder(String placeholder) {
        return By.xpath("//input[@placeholder=" + quote(placeholder) + "] | //textarea[@placeholder=" + quote(placeholder) + "]");
    }

    public static By text(String text) {
        return By.xpath("//*[contains(normalize-space(.), " + quote(text) + ")]");
    }

    public static By exactText(String text) {
        return By.xpath("//*[normalize-space(.)=" + quote(text) + "]");
    }

    public static By button(String text) {
        return By.xpath("//button[contains(normalize-space(.), " + quote(text) + ")]");
    }

    public static By linkOrButton(String text) {
        return By.xpath("//a[contains(normalize-space(.), " + quote(text) + ")] | //button[contains(normalize-space(.), " + quote(text) + ")]");
    }

    public static By toastContaining(String text) {
        return By.xpath("//*[contains(@class,'toast') or @role='status' or @aria-live='polite' or contains(@class,'go')][contains(normalize-space(.), " + quote(text) + ")]");
    }

    public static By validationMessageContaining(String text) {
        return By.xpath("//*[contains(@class,'text-red') or contains(@class,'destructive') or contains(@class,'error') or @role='alert'][contains(normalize-space(.), " + quote(text) + ")]");
    }

    private static String quote(String value) {
        if (!value.contains("'")) {
            return "'" + value + "'";
        }
        if (!value.contains("\"")) {
            return "\"" + value + "\"";
        }
        return "concat('" + value.replace("'", "',\"'\",'") + "')";
    }
}
