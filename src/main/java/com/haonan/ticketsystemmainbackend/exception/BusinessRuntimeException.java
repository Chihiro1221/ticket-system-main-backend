package com.haonan.ticketsystemmainbackend.exception;

import com.haonan.ticketsystemmainbackend.common.ResponseCode;
import lombok.Getter;

/**
 * 业务运行时异常
 * 用于封装业务逻辑中的异常情况
 *
 * @author heart
 */
@Getter
public class BusinessRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误消息
     */
    private final String message;

    /**
     * 构造函数（默认错误码）
     *
     * @param message 错误消息
     */
    public BusinessRuntimeException(String message) {
        super(message);
        this.code = ResponseCode.FAIL.getCode();
        this.message = message;
    }

    /**
     * 构造函数（指定错误码）
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public BusinessRuntimeException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 构造函数（指定响应状态码枚举）
     *
     * @param responseCode 响应状态码枚举
     */
    public BusinessRuntimeException(ResponseCode responseCode) {
        super(responseCode.getMessage());
        this.code = responseCode.getCode();
        this.message = responseCode.getMessage();
    }

    /**
     * 构造函数（指定响应状态码枚举和自定义消息）
     *
     * @param responseCode 响应状态码枚举
     * @param message      自定义消息
     */
    public BusinessRuntimeException(ResponseCode responseCode, String message) {
        super(message);
        this.code = responseCode.getCode();
        this.message = message;
    }

    /**
     * 构造函数（包含异常链）
     *
     * @param message 错误消息
     * @param cause   原因
     */
    public BusinessRuntimeException(String message, Throwable cause) {
        super(message, cause);
        this.code = ResponseCode.FAIL.getCode();
        this.message = message;
    }

    /**
     * 构造函数（指定错误码、错误消息和异常链）
     *
     * @param code    错误码
     * @param message 错误消息
     * @param cause   原因
     */
    public BusinessRuntimeException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    /**
     * 构造函数（指定响应状态码枚举、自定义消息和异常链）
     *
     * @param responseCode 响应状态码枚举
     * @param message      自定义消息
     * @param cause        原因
     */
    public BusinessRuntimeException(ResponseCode responseCode, String message, Throwable cause) {
        super(message, cause);
        this.code = responseCode.getCode();
        this.message = message;
    }
}
