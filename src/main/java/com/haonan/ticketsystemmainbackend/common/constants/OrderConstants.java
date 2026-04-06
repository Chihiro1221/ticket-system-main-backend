package com.haonan.ticketsystemmainbackend.common.constants;

/**
 * 订单相关常量
 * 定义订单状态、订单消息等常量
 *
 * @author heart
 */
public final class OrderConstants {

    private OrderConstants() {
        throw new AssertionError("常量类不允许实例化");
    }

    // ========== 订单状态 ==========

    /**
     * 订单状态：待支付
     */
    public static final int ORDER_STATUS_PENDING = 0;

    /**
     * 订单状态：已支付
     */
    public static final int ORDER_STATUS_PAID = 1;

    /**
     * 订单状态：已取消
     */
    public static final int ORDER_STATUS_CANCELLED = 3;

    // ========== 订单状态字符串（用于状态存储） ==========

    /**
     * 订单状态字符串：待支付
     */
    public static final String ORDER_STATUS_STR_PENDING = "PENDING";

    /**
     * 订单状态字符串：已支付
     */
    public static final String ORDER_STATUS_STR_PAID = "PAID";

    // ========== 订单消息 ==========

    /**
     * 下单成功消息
     */
    public static final String MSG_ORDER_SUCCESS = "下单成功，请在10分钟内完成支付";

}
