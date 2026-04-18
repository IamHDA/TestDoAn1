package com.vn.backend.configs;

public class DeviceInfoContext {
    private static final ThreadLocal<String> deviceInfoHolder = new ThreadLocal<>();

    public static void set(String deviceInfo) {
        deviceInfoHolder.set(deviceInfo);
    }

    public static String get() {
        return deviceInfoHolder.get();
    }

    public static void clear() {
        deviceInfoHolder.remove();
    }
}

