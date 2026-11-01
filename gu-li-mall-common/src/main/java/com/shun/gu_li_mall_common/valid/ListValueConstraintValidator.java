package com.shun.gu_li_mall_common.valid;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link ListValue} 校验器
 *
 * @author yehuoshun
 */
public class ListValueConstraintValidator implements ConstraintValidator<ListValue, Integer> {

    private final Set<Integer> allowed = new HashSet<>();

    @Override
    public void initialize(ListValue constraintAnnotation) {
        for (int v : constraintAnnotation.vals()) {
            allowed.add(v);
        }
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        // null 是否合法交给其它注解（如 @NotNull）决定
        return value == null || allowed.contains(value);
    }
}
