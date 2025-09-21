package com.blog.config;


import com.blog.common.exception.BusinessException;
import com.blog.common.enums.SystemErrorCode;
import com.blog.common.model.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理自定义的业务异常
     * @param ex BusinessException
     * @return 统一响应
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK) // 对于业务逻辑错误，我们倾向于返回200，让前端根据code处理
    public Result<?> handleBusinessException(BusinessException ex) {
        // log.warn("业务异常 -> Code: {}, Message: {}", ex.getErrorCode().getCode(), ex.getMessage());
        return Result.error(ex.getErrorCode());
    }

    /**
     * 处理 Spring Validation 校验失败的异常 (@RequestBody)
     * @param ex MethodArgumentNotValidException
     * @return 统一响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 参数校验失败是客户端的责任，返回 400
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        BindingResult bindingResult = ex.getBindingResult();
        // 使用 Java 8 Stream API 优雅地拼接错误信息
        String errorMsg = bindingResult.getFieldErrors().stream()
                .map(fieldError -> String.format("'%s': %s", fieldError.getField(), fieldError.getDefaultMessage()))
                .collect(Collectors.joining("; "));

        log.warn("参数校验失败 -> {}", errorMsg);
        return Result.error(SystemErrorCode.VALIDATION_ERROR, errorMsg);
    }

    /**
     * 处理其他所有未被捕获的系统异常
     * @param ex       Exception
     * @param request  HttpServletRequest
     * @return 统一响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // 未知异常是服务端的责任，返回 500
    public Result<?> handleUnexpectedException(Exception ex, HttpServletRequest request) {
        // 使用 commons-lang3 的 ExceptionUtils 来获取根异常和完整的堆栈信息
        Throwable rootCause = ExceptionUtils.getRootCause(ex);
        String rootCauseMessage = rootCause != null ? rootCause.getMessage() : ex.getMessage();
        
        // 记录详尽的错误日志，包含请求信息和堆栈，便于排查
        log.error("系统未知异常 -> Request URI: {}, Method: {}, RootCause: {}",
                request.getRequestURI(), request.getMethod(), rootCauseMessage, ex);

        // 对外只暴露通用的、安全的错误提示
        return Result.error(SystemErrorCode.SYSTEM_ERROR);
    }
}
