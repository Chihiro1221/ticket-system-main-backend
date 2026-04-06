package com.haonan.ticketsystemmainbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录请求
 */
@Data
public class UserLoginRequest {

    /**
     * 账号名
     */
    @NotBlank(message = "账号名不能为空")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
