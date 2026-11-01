package com.shun.gu_li_mall_common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.text.SimpleDateFormat;

/**
 * Jackson 工具类（替代旧 fastjson 的一切用法）
 *
 * <p>时间格式默认 {@code yyyy-MM-dd HH:mm:ss} 与旧接口保持一致；
 * 后续如需 LocalDateTime 序列化，可再引入 jsr310 模块。</p>
 *
 * @author yehuoshun
 */
public final class JacksonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    private JacksonUtils() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    /** 对象 → JSON 字符串 */
    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 序列化失败: " + obj, e);
        }
    }

    /** JSON 字符串 → 对象（按 Class） */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("JSON 反序列化失败: " + json, e);
        }
    }

    /** JSON 字符串 → 对象（按 TypeReference，支持泛型） */
    public static <T> T fromJson(String json, TypeReference<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException("JSON 反序列化失败: " + json, e);
        }
    }

    /** JSON 字符串 → JsonNode */
    public static JsonNode readTree(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("JSON 解析失败: " + json, e);
        }
    }
}
