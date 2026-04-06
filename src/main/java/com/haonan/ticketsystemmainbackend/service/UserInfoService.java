package com.haonan.ticketsystemmainbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.haonan.ticketsystemmainbackend.domain.UserInfo;
import com.haonan.ticketsystemmainbackend.dto.UserLoginRequest;
import com.haonan.ticketsystemmainbackend.dto.UserLoginResponse;
import com.haonan.ticketsystemmainbackend.dto.UserProfileResponse;
import com.haonan.ticketsystemmainbackend.dto.UserRegisterRequest;
import com.haonan.ticketsystemmainbackend.dto.UserUpdateRequest;
import java.util.List;

/**
 * 用户 Service
 */
public interface UserInfoService extends IService<UserInfo> {

    UserProfileResponse register(UserRegisterRequest request);

    UserLoginResponse login(UserLoginRequest request);

    UserProfileResponse getUserProfile(String userId);

    UserProfileResponse updateUserProfile(String userId, UserUpdateRequest request);

    List<UserProfileResponse> listUsers(String username, String role);
}
