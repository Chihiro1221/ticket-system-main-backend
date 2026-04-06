package com.haonan.ticketsystemmainbackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求
 */
@Data
public class UserRegisterRequest {

    /**
     * 账号名
     */
    @NotBlank(message = "账号名不能为空")
    @Size(min = 4, max = 32, message = "账号名长度需在4到32位之间")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需在6到64位之间")
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 手机号
     */
    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 票务偏好信息
     */
    private String ticketPreferences;
}
