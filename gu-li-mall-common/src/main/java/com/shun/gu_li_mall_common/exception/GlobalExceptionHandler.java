package com.shun.gu_li_mall_common.exception;

import com.shun.gu_li_mall_common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局异常处理（统一兜底）
 *
 * <p>覆盖三类：@RequestBody 校验 / GET·表单绑定校验（旧版遗漏）/ 未知异常。</p>
 *
 * @author yehuoshun
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.shun.gu_li_mall")
public class GlobalExceptionHandler {

    /** @RequestBody 参数校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Map<String, String>> handleValidException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(fe -> errors.put(fe.getField(), fe.getDefaultMessage()));
        log.warn("参数校验失败: {}", errors);
        return R.error(BizCodeEnum.VALID_EXCEPTION).put("data", errors);
    }

    /** GET/表单绑定参数校验失败（旧版未覆盖的入口） */
    @ExceptionHandler(BindException.class)
    public R<Map<String, String>> handleBindException(BindException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(fe -> errors.put(fe.getField(), fe.getDefaultMessage()));
        log.warn("参数绑定校验失败: {}", errors);
        return R.error(BizCodeEnum.VALID_EXCEPTION).put("data", errors);
    }

    /** 未知异常兜底 */
    @ExceptionHandler(Throwable.class)
    public R<Void> handleException(Throwable e) {
        log.error("系统异常", e);
        return R.error(BizCodeEnum.UNKNOWN_EXCEPTION);
    }
}
