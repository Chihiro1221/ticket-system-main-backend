package com.haonan.ticketsystemmainbackend.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一响应结果封装
 *
 * @param <T> 数据类型
 * @author heart
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        return success(ResponseCode.SUCCESS.getMessage(), data);
    }

    /**
     * 成功响应（自定义消息）
     */
    public static <T> Result<T> success(String message) {
        return success(message, null);
    }

    /**
     * 成功响应（自定义消息和数据）
     */
    public static <T> Result<T> success(String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(ResponseCode.SUCCESS.getCode());
        result.setMessage(message);
        result.setData(data);
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }

    /**
     * 失败响应（默认错误码）
     */
    public static <T> Result<T> fail() {
        return fail(ResponseCode.FAIL);
    }

    /**
     * 失败响应（指定错误码）
     */
    public static <T> Result<T> fail(ResponseCode responseCode) {
        return fail(responseCode, null);
    }

    /**
     * 失败响应（指定错误码和自定义消息）
     */
    public static <T> Result<T> fail(ResponseCode responseCode, String message) {
        Result<T> result = new Result<>();
        result.setCode(responseCode.getCode());
        result.setMessage(message != null ? message : responseCode.getMessage());
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }

    /**
     * 失败响应（自定义错误码和消息）
     */
    public static <T> Result<T> fail(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }
}
