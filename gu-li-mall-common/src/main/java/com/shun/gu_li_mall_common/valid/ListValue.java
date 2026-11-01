package com.shun.gu_li_mall_common.valid;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义校验注解：值必须在指定枚举列表中
 *
 * <p>用法：{@code @ListValue(vals = {0, 1}, groups = {AddGroup.class})}</p>
 *
 * @author yehuoshun
 */
@Documented
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ListValueConstraintValidator.class)
public @interface ListValue {

    int[] vals() default {};

    String message() default "必须为指定值";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
