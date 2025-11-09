package com.blog.handler;


import com.blog.common.exception.BusinessException;
import com.blog.common.enums.SystemErrorCode;
import com.blog.common.exception.EntityNotFoundException;
import com.blog.common.exception.OperationFailedException;
import com.blog.common.model.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理自定义的业务异常
     *
     * @param ex BusinessException
     * @return 统一响应
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK) // 对于业务逻辑错误，我们倾向于返回200，让前端根据code处理
    public Result<?> handleBusinessException(BusinessException ex) {
        log.warn("业务异常 -> Code: {}, Message: {}", ex.getErrorCode().getCode(), ex.getMessage());
        return Result.error(ex.getErrorCode());
    }

    /**
     * 处理 Spring Validation 校验失败的异常 (@RequestBody)
     * <p>
     * 返回结构化的错误信息，方便前端直接使用。
     *
     * @param ex MethodArgumentNotValidException
     * @return 统一响应，其中 data 字段为 { "fieldName": "errorMessage", ... }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        BindingResult bindingResult = ex.getBindingResult();
        // 核心改动：在 valueMapper 中增加对 null 的处理，防止 Collectors.toMap 抛出 NPE
        Map<String, String> errorMap = bindingResult.getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,      // key: 字段名 (方法引用)
                        fieldError -> {            // value: 错误信息 (Lambda)
                            String message = fieldError.getDefaultMessage();
                            // 如果 message 为 null，则返回一个空字符串，保证健壮性
                            return message == null ? "" : message;
                        },
                        (oldValue, newValue) -> oldValue // 如果同一个字段有多个错误，只保留第一个
                ));
        log.warn("参数校验失败 -> {}", errorMap);

        return Result.error(SystemErrorCode.VALIDATION_ERROR, errorMap);
    }

    /**
     * 处理实体未找到异常，返回 404 Not Found
     *
     * @param ex EntityNotFoundException
     * @return 统一响应
     */
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<?> handleEntityNotFoundException(EntityNotFoundException ex) {
        log.warn("实体未找到异常 -> Code: {}, Message: {}", ex.getErrorCode().getCode(), ex.getMessage());
        return Result.error(ex.getErrorCode());
    }

    /**
     * 处理数据库操作失败异常
     *
     * @param ex OperationFailedException
     * @return 统一响应
     */
    @ExceptionHandler(OperationFailedException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // 操作失败是服务端问题，返回 500
    public Result<?> handleOperationFailedException(OperationFailedException ex) {
        // 使用 error 级别记录，因为它通常表示一个需要关注的服务端问题
        log.error("操作失败异常 -> Code: {}, Message: {}, Payload: {}",
                ex.getErrorCode().getCode(),
                ex.getMessage(),
                // 记录下导致失败的数据，极大地帮助问题排查
                ex.getPayload() != null ? ex.getPayload().toString() : "N/A",
                ex // 打印堆栈信息
        );
        // 你可以选择只返回通用错误信息，或将异常 message 返回给前端
        return Result.error(ex.getErrorCode(), ex.getMessage());
    }

    /**
     * 【新增】处理 Spring MVC 抛出的资源未找到异常 (404 Not Found)
     * <p>
     * 这个处理器专门拦截 NoResourceFoundException，
     * 防止它被最后的通用 ExceptionHandler (handleUnexpectedException) 捕获，
     * 从而确保返回正确的 404 状态码，而不是 500。
     *
     * @param ex      NoResourceFoundException
     * @param request HttpServletRequest
     * @return 统一响应体，带有 404 语义
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND) // 明确指定HTTP状态码为 404
    public Result<?> handleNoResourceFoundException(org.springframework.web.servlet.resource.NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("资源未找到 -> Request URI: {}, Method: {}", request.getRequestURI(), request.getMethod());
        // 返回一个符合你项目规范的、带有404 语义的响应体
        return Result.error(SystemErrorCode.NOT_FOUND);
    }

    /**
     * 处理其他所有未被捕获的系统异常
     *
     * @param ex      Exception
     * @param request HttpServletRequest
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
