package com.haonan.ticketsystemmainbackend.common.constants;

/**
 * Dapr 组件相关常量
 * 定义 Dapr 状态存储、消息队列、锁等组件的名称和配置
 *
 * @author heart
 */
public final class DaprConstants {

    private DaprConstants() {
        throw new AssertionError("常量类不允许实例化");
    }

    // ========== 状态存储组件 ==========

    /**
     * Dapr 状态存储组件名称
     */
    public static final String STATE_STORE_NAME = "statestore";

    // ========== 消息队列组件 ==========

    /**
     * Dapr PubSub 组件名称
     */
    public static final String PUBSUB_NAME = "pubsub";

    /**
     * 订单主题名称
     */
    public static final String TOPIC_ORDER = "order_topic";

    // ========== 分布式锁组件 ==========

    /**
     * 分布式锁存储组件名称
     */
    public static final String LOCK_STORE_NAME = "ticket-lock-store";

    // ========== Key 格式模板 ==========

    /**
     * 购买记录 Key 格式：ticket_purchase_record:{userId}:{ticketId}
     */
    public static final String KEY_PURCHASE_RECORD_FORMAT = "ticket_purchase_record:%s:%s";

    /**
     * 用户锁资源 ID 前缀
     */
    public static final String USER_LOCK_PREFIX = "user_lock_";

    /**
     * 限购记录 Key 格式（旧版）：purchase_record:{userId}:{ticketId}
     */
    public static final String KEY_PURCHASE_RECORD_LEGACY_FORMAT = "purchase_record:%s:%s";

}
