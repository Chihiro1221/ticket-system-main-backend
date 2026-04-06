package com.haonan.ticketsystemmainbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginResponse {

    /**
     * JWT token
     */
    private String token;

    /**
     * token 类型
     */
    private String tokenType;

    /**
     * 过期时间（秒）
     */
    private Long expiresIn;

    /**
     * 用户信息
     */
    private UserProfileResponse userInfo;
}
