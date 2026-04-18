package com.vn.backend.utils;

public class EnumUtils {

  private EnumUtils() {
  }

  public static <E extends Enum<E>> E fromString(Class<E> enumType, String value) {
    if (value == null) {
      return null;
    }
    try {
      return Enum.valueOf(enumType, value.toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
