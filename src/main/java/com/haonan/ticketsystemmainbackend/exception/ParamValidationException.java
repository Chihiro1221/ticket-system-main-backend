package com.haonan.ticketsystemmainbackend.exception;

import lombok.Getter;

/**
 * 参数校验异常
 * 用于封装参数校验失败的异常情况
 *
 * @author heart
 */
@Getter
public class ParamValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误消息
     */
    private final String message;

    /**
     * 构造函数
     *
     * @param message 错误消息
     */
    public ParamValidationException(String message) {
        super(message);
        this.message = message;
    }

    /**
     * 构造函数（包含异常链）
     *
     * @param message 错误消息
     * @param cause   原因
     */
    public ParamValidationException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
    }
}
