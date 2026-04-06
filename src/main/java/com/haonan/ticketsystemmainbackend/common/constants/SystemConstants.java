package com.haonan.ticketsystemmainbackend.common.constants;

/**
 * 系统通用常量
 * 定义系统级别的常量，如超时时间、默认值等
 *
 * @author heart
 */
public final class SystemConstants {

    private SystemConstants() {
        throw new AssertionError("常量类不允许实例化");
    }

    // ========== 时间相关常量（单位：秒） ==========

    /**
     * 订单超时时间（10分钟）
     */
    public static final int ORDER_TIMEOUT_MINUTES = 15;

    /**
     * 订单超时时间（秒）
     */
    public static final int ORDER_TIMEOUT_SECONDS = 900;

    /**
     * 状态过期时间（15分钟 = 900秒）
     */
    public static final int STATE_EXPIRY_SECONDS = 900;

    /**
     * 分布式锁过期时间（秒）
     */
    public static final int LOCK_EXPIRY_SECONDS = 30;

    // ========== 库存相关常量 ==========

    /**
     * 默认库存数量
     */
    public static final int DEFAULT_STOCK = 100;

    /**
     * 库存扣减数量
     */
    public static final int STOCK_DEDUCT_COUNT = 1;

    /**
     * 库存回滚数量
     */
    public static final int STOCK_RESTORE_COUNT = 1;

    // ========== Actor 相关常量 ==========

    /**
     * Actor 状态键：库存数量
     */
    public static final String ACTOR_STOCK_KEY = "stock_count";

    /**
     * 提醒器周期：只执行一次
     */
    public static final long REMINDER_PERIOD_ONCE = -1;

    /**
     * 提醒器名称前缀
     */
    public static final String REMINDER_PREFIX_ORDER_TIMEOUT = "ORDER_TIMEOUT_";

}
