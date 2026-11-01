package com.shun.gu_li_mall_common.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期工具（java.time 风格，线程安全）
 *
 * @author yehuoshun
 */
@SuppressWarnings("unused")
public final class DateUtils {

    public static final String PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(PATTERN);

    private DateUtils() {
    }

    /** LocalDateTime → "yyyy-MM-dd HH:mm:ss" */
    public static String format(LocalDateTime dateTime) {
        return dateTime == null ? null : FORMATTER.format(dateTime);
    }

    /** "yyyy-MM-dd HH:mm:ss" → LocalDateTime */
    public static LocalDateTime parse(String text) {
        return text == null ? null : LocalDateTime.parse(text, FORMATTER);
    }

    /** 今天日期 "yyyy-MM-dd" */
    public static String today() {
        return LocalDate.now().toString();
    }
}
