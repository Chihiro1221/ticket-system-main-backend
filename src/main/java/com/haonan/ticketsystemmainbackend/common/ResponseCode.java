package com.haonan.ticketsystemmainbackend.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应状态码枚举
 *
 * @author heart
 */
@Getter
@AllArgsConstructor
public enum ResponseCode {

    // ========== 通用状态码 ==========

    /**
     * 操作成功
     */
    SUCCESS(200, "操作成功"),

    /**
     * 操作失败
     */
    FAIL(500, "操作失败"),

    // ========== 客户端错误 4xx ==========

    /**
     * 参数校验失败
     */
    BAD_REQUEST(400, "参数校验失败"),

    /**
     * 未授权
     */
    UNAUTHORIZED(401, "未授权，请先登录"),

    /**
     * 禁止访问
     */
    FORBIDDEN(403, "禁止访问"),

    /**
     * 资源不存在
     */
    NOT_FOUND(404, "资源不存在"),

    /**
     * 请求方法不支持
     */
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    /**
     * 请求超时
     */
    REQUEST_TIMEOUT(408, "请求超时"),

    /**
     * 请求过于频繁
     */
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),

    // ========== 服务端错误 5xx ==========

    /**
     * 服务器内部错误
     */
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),

    /**
     * 服务不可用
     */
    SERVICE_UNAVAILABLE(503, "服务不可用"),

    // ========== 业务错误码（自定义） ==========

    /**
     * 库存不足
     */
    STOCK_INSUFFICIENT(1001, "库存不足"),

    /**
     * 用户已购买该场次
     */
    USER_ALREADY_PURCHASED(1002, "您已经购买过该场次，请勿重复下单"),

    /**
     * 用户操作过于频繁
     */
    USER_OPERATION_TOO_FREQUENT(1003, "您的操作太频繁或有正在处理的订单，请稍后再试"),

    /**
     * 订单不存在
     */
    ORDER_NOT_FOUND(1004, "订单不存在"),

    /**
     * 订单状态异常
     */
    ORDER_STATUS_ERROR(1005, "订单状态异常"),

    /**
     * 订单已超时
     */
    ORDER_TIMEOUT(1006, "订单已超时"),

    /**
     * 订单已支付
     */
    ORDER_ALREADY_PAID(1007, "订单已支付"),

    /**
     * 订单已取消
     */
    ORDER_ALREADY_CANCELLED(1008, "订单已取消"),

    /**
     * 支付失败
     */
    PAYMENT_FAILED(1009, "支付失败"),

    /**
     * 库存扣减失败
     */
    STOCK_DEDUCTION_FAILED(1010, "库存扣减失败"),

    /**
     * 购买记录不存在
     */
    PURCHASE_RECORD_NOT_FOUND(1011, "购买记录不存在"),

    /**
     * 票务不存在
     */
    TICKET_NOT_FOUND(1012, "票务不存在"),

    /**
     * 用户不存在
     */
    USER_NOT_FOUND(1013, "用户不存在"),

    /**
     * 用户名已存在
     */
    USERNAME_ALREADY_EXISTS(1014, "账号名已存在"),

    /**
     * 用户名或密码错误
     */
    USERNAME_OR_PASSWORD_ERROR(1015, "账号名或密码错误"),

    /**
     * 用户已被封禁
     */
    USER_BANNED(1016, "当前用户已被封禁"),

    /**
     * 数据库操作失败
     */
    DATABASE_ERROR(2001, "数据库操作失败"),

    /**
     * 缓存操作失败
     */
    CACHE_ERROR(2002, "缓存操作失败"),

    /**
     * 消息队列发送失败
     */
    MESSAGE_QUEUE_ERROR(2003, "消息队列发送失败"),

    /**
     * 分布式锁获取失败
     */
    LOCK_ACQUISITION_FAILED(2004, "分布式锁获取失败"),

    /**
     * Actor 调用失败
     */
    ACTOR_INVOCATION_FAILED(2005, "Actor 调用失败"),

    /**
     * 未知错误
     */
    UNKNOWN_ERROR(9999, "未知错误");

    /**
     * 响应码
     */
    private final Integer code;

    /**
     * 响应消息
     */
    private final String message;
}
