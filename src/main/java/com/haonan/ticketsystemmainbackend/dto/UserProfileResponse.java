package com.haonan.ticketsystemmainbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户信息响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private String userId;

    private String username;

    private String role;

    private String nickname;

    private String avatar;

    private String email;

    private String phone;

    private String ticketPreferences;
}
