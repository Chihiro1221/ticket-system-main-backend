package com.haonan.ticketsystemmainbackend.exception;

import com.haonan.ticketsystemmainbackend.common.ResponseCode;
import com.haonan.ticketsystemmainbackend.common.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理各类异常，返回标准格式的错误响应
 *
 * @author heart
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理业务运行时异常
     */
    @ExceptionHandler(BusinessRuntimeException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleBusinessRuntimeException(BusinessRuntimeException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常（@RequestBody）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验异常: {}", errorMessage);
        return Result.fail(ResponseCode.BAD_REQUEST.getCode(), errorMessage);
    }

    /**
     * 处理参数绑定异常（@ModelAttribute）
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleBindException(BindException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定异常: {}", errorMessage);
        return Result.fail(ResponseCode.BAD_REQUEST.getCode(), errorMessage);
    }

    /**
     * 处理约束违反异常（@Validated 单个参数校验）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleConstraintViolationException(ConstraintViolationException e) {
        String errorMessage = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("约束违反异常: {}", errorMessage);
        return Result.fail(ResponseCode.BAD_REQUEST.getCode(), errorMessage);
    }

    /**
     * 处理缺少请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        String errorMessage = String.format("缺少必需的请求参数: %s", e.getParameterName());
        log.warn(errorMessage);
        return Result.fail(ResponseCode.BAD_REQUEST.getCode(), errorMessage);
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String errorMessage = String.format("参数类型不匹配: 参数名=%s, 期望类型=%s, 实际值=%s",
                e.getName(), e.getRequiredType().getSimpleName(), e.getValue());
        log.warn(errorMessage);
        return Result.fail(ResponseCode.BAD_REQUEST.getCode(), errorMessage);
    }

    /**
     * 处理请求消息不可读异常（JSON 解析错误）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("请求消息不可读: {}", e.getMessage());
        return Result.fail(ResponseCode.BAD_REQUEST.getCode(), "请求参数格式错误，请检查JSON格式");
    }

    /**
     * 处理请求方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<?> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        String errorMessage = String.format("不支持 %s 请求方法", e.getMethod());
        log.warn(errorMessage);
        return Result.fail(ResponseCode.METHOD_NOT_ALLOWED.getCode(), errorMessage);
    }

    /**
     * 处理 404 异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<?> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.warn("请求路径不存在: {} {}", e.getHttpMethod(), e.getRequestURL());
        return Result.fail(ResponseCode.NOT_FOUND);
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数异常: {}", e.getMessage());
        return Result.fail(ResponseCode.BAD_REQUEST.getCode(), e.getMessage());
    }

    /**
     * 处理非法状态异常
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleIllegalStateException(IllegalStateException e) {
        log.error("非法状态异常", e);
        return Result.fail(ResponseCode.INTERNAL_SERVER_ERROR.getCode(), "系统状态异常");
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleNullPointerException(NullPointerException e) {
        log.error("空指针异常", e);
        return Result.fail(ResponseCode.INTERNAL_SERVER_ERROR.getCode(), "系统内部错误");
    }

    /**
     * 处理其他未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ResponseCode.INTERNAL_SERVER_ERROR.getCode(), "系统内部错误，请联系管理员");
    }
}
