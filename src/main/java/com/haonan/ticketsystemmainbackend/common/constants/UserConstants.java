package com.haonan.ticketsystemmainbackend.common.constants;

/**
 * 用户相关常量
 */
public final class UserConstants {

    private UserConstants() {
        throw new AssertionError("常量类不允许实例化");
    }

    /**
     * 管理员角色
     */
    public static final String ROLE_ADMIN = "admin";

    /**
     * 普通用户角色
     */
    public static final String ROLE_USER = "user";

    /**
     * 封禁角色
     */
    public static final String ROLE_BAN = "ban";

    /**
     * JWT Bearer 前缀
     */
    public static final String TOKEN_TYPE_BEARER = "Bearer";
}
