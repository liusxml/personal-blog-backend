package com.blog.handler;

import com.blog.common.exception.BusinessException;
import com.blog.common.exception.EntityNotFoundException;
import com.blog.common.exception.OperationFailedException;
import com.blog.common.exception.SystemErrorCode;
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

/**
 * 全局异常处理器
 * <p>
 * 负责捕获和处理应用中所有未被捕获的异常，统一返回格式化的错误响应。
 * 
 * <h3>异常处理优先级（按 HTTP 状态码分组）：</h3>
 * <ul>
 * <li><b>200 OK</b> - 业务异常（BusinessException）</li>
 * <li><b>400 Bad Request</b> - 参数校验失败（MethodArgumentNotValidException）</li>
 * <li><b>401 Unauthorized</b> - 未认证（AuthenticationException）</li>
 * <li><b>403 Forbidden</b> - 无权访问（AuthorizationDeniedException）</li>
 * <li><b>404 Not Found</b> - 资源未找到（EntityNotFoundException,
 * NoResourceFoundException）</li>
 * <li><b>500 Internal Server Error</b> -
 * 操作失败（OperationFailedException）、未知异常（Exception）</li>
 * </ul>
 * 
 * <h3>日志级别规范：</h3>
 * <ul>
 * <li><b>WARN</b> - 业务异常、参数错误、权限错误（可预期的异常）</li>
 * <li><b>ERROR</b> - 系统异常、操作失败（需要关注的异常）</li>
 * </ul>
 *
 * @author liusxml
 * @version 2.0
 * @since 1.0.0
 * @see BusinessException
 * @see SystemErrorCode
 * @see Result
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String LOG_TEMPLATE = "{} -> URI: {}, Method: {}";
    private static final String LOG_TEMPLATE_WITH_CODE = "{} -> URI: {}, Method: {}, ErrorCode: {}";

    // ========================================
    // 2xx - 成功响应（业务异常使用 200）
    // ========================================

    /**
     * 处理业务逻辑异常
     * <p>
     * 业务异常返回 200 状态码，由前端根据 code 字段判断具体错误。
     * 这种设计使得前端可以统一处理响应，无需区分 HTTP 状态码。
     *
     * @param ex 业务异常
     * @return 错误响应，包含业务错误码和消息
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleBusinessException(BusinessException ex) {
        log.warn(LOG_TEMPLATE_WITH_CODE, "业务异常",
                getCurrentRequest().getRequestURI(),
                getCurrentRequest().getMethod(),
                ex.getErrorCode().getCode());

        // 如果有自定义消息，使用自定义消息；否则使用 errorCode 的默认消息
        String message = ex.getMessage();
        if (message != null && !message.equals(ex.getErrorCode().getMessage())) {
            return Result.error(ex.getErrorCode(), message);
        }
        return Result.error(ex.getErrorCode());
    }

    // ========================================
    // 4xx - 客户端错误
    // ========================================

    /**
     * 处理参数校验失败异常
     * <p>
     * 返回字段级别的错误信息，格式为：{ "fieldName": "errorMessage", ... }
     * 便于前端直接展示在对应的表单字段下。
     *
     * @param ex 参数校验异常
     * @return 错误响应，data 字段包含字段错误映射
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleValidationException(MethodArgumentNotValidException ex) {
        BindingResult bindingResult = ex.getBindingResult();

        Map<String, String> errorMap = bindingResult.getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "",
                        (existing, replacement) -> existing));

        log.warn(LOG_TEMPLATE + ", Errors: {}", "参数校验失败",
                getCurrentRequest().getRequestURI(),
                getCurrentRequest().getMethod(),
                errorMap);

        return Result.error(SystemErrorCode.VALIDATION_ERROR, errorMap);
    }

    /**
     * 处理认证失败异常
     * <p>
     * 当用户未登录或 Token 无效时触发。
     *
     * @param ex      认证异常
     * @param request HTTP 请求
     * @return 错误响应，401 Unauthorized
     */
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<?> handleAuthenticationException(
            org.springframework.security.core.AuthenticationException ex,
            HttpServletRequest request) {
        log.warn(LOG_TEMPLATE, "认证失败", request.getRequestURI(), request.getMethod());
        return Result.error(SystemErrorCode.UNAUTHORIZED);
    }

    /**
     * 处理授权失败异常
     * <p>
     * 当用户已认证但无权访问某资源时触发。
     *
     * @param ex      授权异常
     * @param request HTTP 请求
     * @return 错误响应，403 Forbidden
     */
    @ExceptionHandler({
            org.springframework.security.authorization.AuthorizationDeniedException.class,
            org.springframework.security.access.AccessDeniedException.class
    })
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<?> handleAuthorizationException(Exception ex, HttpServletRequest request) {
        log.warn(LOG_TEMPLATE, "访问被拒绝", request.getRequestURI(), request.getMethod());
        return Result.error(SystemErrorCode.ACCESS_DENIED);
    }

    /**
     * 处理实体未找到异常
     * <p>
     * 当查询的业务实体不存在时触发。
     *
     * @param ex 实体未找到异常
     * @return 错误响应，404 Not Found
     */
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<?> handleEntityNotFoundException(EntityNotFoundException ex) {
        log.warn(LOG_TEMPLATE_WITH_CODE, "实体未找到",
                getCurrentRequest().getRequestURI(),
                getCurrentRequest().getMethod(),
                ex.getErrorCode().getCode());
        return Result.error(ex.getErrorCode());
    }

    /**
     * 处理静态资源未找到异常
     * <p>
     * 当请求的 URL 路径不存在时触发（Spring MVC 层面）。
     * 需要在业务层的 EntityNotFoundException 之后处理，避免误拦截。
     *
     * @param ex      资源未找到异常
     * @param request HTTP 请求
     * @return 错误响应，404 Not Found
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<?> handleNoResourceFoundException(NoResourceFoundException ex, HttpServletRequest request) {
        log.warn(LOG_TEMPLATE, "资源未找到", request.getRequestURI(), request.getMethod());
        return Result.error(SystemErrorCode.NOT_FOUND);
    }

    // ========================================
    // 5xx - 服务端错误
    // ========================================

    /**
     * 处理数据库操作失败异常
     * <p>
     * 记录完整的错误信息和导致失败的数据载荷，便于问题排查。
     *
     * @param ex 操作失败异常
     * @return 错误响应，500 Internal Server Error
     */
    @ExceptionHandler(OperationFailedException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleOperationFailedException(OperationFailedException ex) {
        log.error(LOG_TEMPLATE_WITH_CODE + ", Payload: {}", "操作失败",
                getCurrentRequest().getRequestURI(),
                getCurrentRequest().getMethod(),
                ex.getErrorCode().getCode(),
                ex.getPayload() != null ? ex.getPayload().toString() : "N/A",
                ex);
        return Result.error(ex.getErrorCode(), ex.getMessage());
    }

    /**
     * 处理所有未被捕获的系统异常
     * <p>
     * 这是最后的兜底处理器，捕获所有其他异常。
     * 记录根异常和完整堆栈信息，但仅向客户端返回通用错误提示，避免泄露敏感信息。
     *
     * @param ex      未知异常
     * @param request HTTP 请求
     * @return 错误响应，500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleUnexpectedException(Exception ex, HttpServletRequest request) {
        Throwable rootCause = ExceptionUtils.getRootCause(ex);
        String rootCauseMessage = rootCause != null ? rootCause.getMessage() : ex.getMessage();

        log.error(LOG_TEMPLATE + ", RootCause: {}", "系统未知异常",
                request.getRequestURI(),
                request.getMethod(),
                rootCauseMessage,
                ex);

        return Result.error(SystemErrorCode.SYSTEM_ERROR);
    }

    // ========================================
    // 私有辅助方法
    // ========================================

    /**
     * 获取当前 HTTP 请求
     * <p>
     * 用于在不需要 HttpServletRequest 参数的处理器中获取请求信息。
     * 通过 RequestContextHolder 从线程上下文中获取。
     *
     * @return 当前 HTTP 请求
     */
    private HttpServletRequest getCurrentRequest() {
        return ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder
                .currentRequestAttributes())
                .getRequest();
    }
}
