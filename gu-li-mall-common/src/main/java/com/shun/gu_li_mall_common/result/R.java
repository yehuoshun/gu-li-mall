package com.shun.gu_li_mall_common.result;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.shun.gu_li_mall_common.exception.BizCodeEnum;
import com.shun.gu_li_mall_common.utils.JacksonUtils;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一返回体（泛型版，替代旧版 {@code extends HashMap} 的硬伤）
 *
 * <p>新代码请用 {@code R.ok(data)} / {@code R.error(...)} 泛型方式；
 * {@link #put(String, Object)} 仅为兼容旧式链式调用保留，JSON 形态与旧版一致。</p>
 *
 * @author yehuoshun
 */
@Data
public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 状态码：0 成功，非 0 失败（码表见 {@link BizCodeEnum}） */
    private Integer code;

    /** 提示信息 */
    private String msg;

    /** 业务数据 */
    private T data;

    /** 扩展键位：经 {@link JsonAnyGetter} 平铺到 JSON 根，保证旧前端字段形态不变 */
    private Map<String, Object> ext = new LinkedHashMap<>();

    // ─── 静态工厂 ───

    public static <T> R<T> ok() {
        return new R<>(0, "success");
    }

    public static <T> R<T> ok(T data) {
        return new R<>(0, "success", data);
    }

    public static <T> R<T> error() {
        return new R<>(500, "未知异常");
    }

    public static <T> R<T> error(String msg) {
        return new R<>(500, msg);
    }

    public static <T> R<T> error(Integer code, String msg) {
        return new R<>(code, msg);
    }

    public static <T> R<T> error(BizCodeEnum enume) {
        return new R<>(enume.getCode(), enume.getMsg());
    }

    // ─── 兼容旧式链式调用（搬旧代码过渡期）───

    /**
     * 兼容旧式链式调用（搬旧代码过渡期）。
     *
     * @param key   键；"data" 写入 {@link #data}，其余写入 {@link #ext}
     * @param value 值
     * @return this（保证链式调用不断）
     * @implNote 泛型擦除使 {@code (T) value} 无法在编译期验证，调用方需自行保证类型一致
     */
    @SuppressWarnings("unchecked")
    public R<T> put(String key, Object value) {
        if ("data".equals(key)) {
            this.data = (T) value;
        } else {
            ext.put(key, value);
        }
        return this;
    }

    public Object get(String key) {
        if ("data".equals(key)) {
            return data;
        }
        return ext.get(key);
    }

    public R<T> setData(T data) {
        this.data = data;
        return this;
    }

    /** 取业务数据（Jackson 版，替代旧 fastJson 用法） */
    public <E> E getData(TypeReference<E> typeReference) {
        return JacksonUtils.fromJson(JacksonUtils.toJson(getData()), typeReference);
    }

    @JsonAnyGetter
    public Map<String, Object> getExt() {
        return ext;
    }

    @SuppressWarnings("unchecked")
    @JsonAnySetter
    public void setExt(String key, Object value) {
        if ("data".equals(key)) {
            this.data = (T) value;
        } else {
            ext.put(key, value);
        }
    }

    public R() {
    }

    public R(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public R(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }
}
