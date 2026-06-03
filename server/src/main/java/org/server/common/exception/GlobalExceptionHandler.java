package org.server.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.server.common.Result;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 1. 🌟 精准拦截你定义的 BusinessException
     * 凡是你在 Service 里手动 throw new BusinessException 的，全都会乖乖来这里报到
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        // 商业规范：由于是可预知的业务逻辑阻止（如分类不存在），用 WARN 级别即可，绝不打印恶心的长堆栈
        log.warn("🚨 业务逻辑拦截 -> code: {}, msg: {}", e.getCode(), e.getMessage());
        // 完美吐出你在构造方法里传入的 code 和 message
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 2. 拦截 DTO 参数校验异常（@Validated / @Valid 没通过时触发）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        // 企业级细节：将前端传错的所有字段提示语用逗号拼接起来，一次性告诉前端（例如："商品名称不能为空, 商品价格不能为负数"）
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("❌ DTO参数验证失败 -> {}", message);
        return Result.error(400, message);
    }

    /**
     * 3. 拦截未知的运行时异常（真正的硬伤 Bug，如空指针 NullPointerException、MyBatis SQL 语法错误）
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        // 商业规范：真正意料之外的 Bug 必须用 ERROR，且必须打印完整的堆栈信息（e），方便线上排查生产问题
        log.error("🔥 系统发生运行时硬伤 Bug：", e);
        // 对外（前端）要绝对模糊化，不能把底层的报错字符吐给用户，防止暴露数据库表结构
        return Result.error(500, "服务器异常，请稍后再试");
    }

    /**
     * 4. 最终全物理兜底拦截
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("💥 系统发生未知致命异常：", e);
        return Result.error(500, "系统未知错误");
    }
}