package com.haonan.ticketsystemmainbackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 用户信息更新请求
 */
@Data
public class UserUpdateRequest {

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
