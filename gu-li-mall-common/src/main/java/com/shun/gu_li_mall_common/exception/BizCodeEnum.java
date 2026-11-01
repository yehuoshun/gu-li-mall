package com.shun.gu_li_mall_common.exception;

import lombok.Getter;

/**
 * 错误码枚举
 *
 * <p>码段规划：10xxx 通用 / 11xxx 商品 / 15xxx 会员 / 21xxx 库存 / 30xxx 认证。
 * 码值沿用课程体系，搬旧代码时无需改业务侧判断。</p>
 *
 * @author yehuoshun
 */
@Getter
public enum BizCodeEnum {

    UNKNOWN_EXCEPTION(10000, "系统未知异常"),
    VALID_EXCEPTION(10001, "参数格式校验失败"),
    SMS_CODE_EXCEPTION(10002, "短信验证码获取频率太高"),
    SMS_CODE_ERROR(10003, "验证码错误"),
    PRODUCT_UP_EXCEPTION(11000, "商品上架异常"),
    USER_EXIST_EXCEPTION(15001, "用户名已存在"),
    PHONE_EXIST_EXCEPTION(15002, "手机号已存在"),
    LOGINACCOUNT_PASSWORD_INVALID_EXCEPTION(15003, "账号或密码错误"),
    NO_STOCK_EXCEPTION(21001, "没有库存"),
    NO_LOGIN_EXCEPTION(30001, "未登录");

    private final int code;
    private final String msg;

    BizCodeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
