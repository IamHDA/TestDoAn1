package com.vn.backend.utils;


import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class DateUtils {

    public static final String YYYY_MM_DD = "yyyy-MM-dd";
    public static final String YYYY_MM_DD_HH_MM = "yyyy-MM-dd HH:mm";
    public static final String YYYY_MM_DD_HH_MM_SS_SSSSSS = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS";
    public static final String YYYY_MM_DD_HH_MM_SS_EXPORT = "yyyyMMddHHmmssSSS";
    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

    private DateUtils() {
    }

    /**
     * Parse string value to local date with patten
     *
     * @param value   string date
     * @param pattern user input
     * @return LocalDateTime
     */
    public static LocalDate parseLocalDate(String value, String pattern) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        DateTimeFormatter formatter = localDateFormat(pattern);
        return LocalDate.parse(value, formatter);
    }

    /**
     * Parse string value to local date time with patten
     *
     * @param value   string date
     * @param pattern user input
     * @return LocalDateTime
     */
    public static LocalDateTime parseLocalDateTime(String value, String pattern) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        DateTimeFormatter formatter = localDateFormat(pattern);
        return LocalDateTime.parse(value, formatter);
    }

    /**
     * Get DateTimeFormatter for LocalDate, LocalDateTime
     *
     * @param formatText string
     * @return DateTimeFormatter
     */
    private static DateTimeFormatter localDateFormat(String formatText) {
        return DateTimeFormatter.ofPattern(formatText);
    }

    /**
     * Is after
     */
    public static boolean isAfter(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            return false;
        }
        return from.isAfter(to);
    }

    /**
     * Compare two LocalDateTime
     */
    public static boolean isSameDateTime(LocalDateTime date1, LocalDateTime date2) {
        if (Objects.isNull(date1)) {
            return Objects.isNull(date2);
        }
        return date1.equals(date2);
    }

    /**
     * Compare two LocalDate
     */
    public static boolean isSameDate(LocalDate date1, LocalDate date2) {
        if (Objects.isNull(date1)) {
            return Objects.isNull(date2);
        }
        return date1.equals(date2);
    }

    /**
     * Format LocalDateTime
     */
    public static String format(LocalDateTime date, String pattern) {
        if (Objects.isNull(date) || StringUtils.isEmpty(pattern)) {
            return null;
        }
        return DateTimeFormatter.ofPattern(pattern).format(date);
    }

    /**
     * Format LocalDate
     */
    public static String format(LocalDate date, String pattern) {
        if (Objects.isNull(date) || StringUtils.isEmpty(pattern)) {
            return null;
        }
        return DateTimeFormatter.ofPattern(pattern).format(date);
    }

    /**
     * Calculate Age
     */
    public static Integer calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }
        LocalDate currentDate = LocalDate.now();
        if (birthDate.isAfter(currentDate)) {
            return null;
        }
        Period period = Period.between(birthDate, currentDate);
        return period.getYears();
    }

    /**
     * Calculate DAY between 2 LocalDateTime
     *
     * @param startDate LocalDateTime
     * @param endDate   LocalDateTime
     * @return
     */
    public static long calculateDaysLength(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(startDate, endDate);
    }

    /**
     * Is after for LocalDate
     */
    public static boolean isAfter(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            return false;
        }
        return from.isAfter(to);
    }
}

