package com.haonan.ticketsystemmainbackend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haonan.ticketsystemmainbackend.common.ResponseCode;
import com.haonan.ticketsystemmainbackend.common.constants.UserConstants;
import com.haonan.ticketsystemmainbackend.config.JwtProperties;
import com.haonan.ticketsystemmainbackend.domain.UserInfo;
import com.haonan.ticketsystemmainbackend.dto.UserLoginRequest;
import com.haonan.ticketsystemmainbackend.dto.UserLoginResponse;
import com.haonan.ticketsystemmainbackend.dto.UserProfileResponse;
import com.haonan.ticketsystemmainbackend.dto.UserRegisterRequest;
import com.haonan.ticketsystemmainbackend.dto.UserUpdateRequest;
import com.haonan.ticketsystemmainbackend.exception.BusinessRuntimeException;
import com.haonan.ticketsystemmainbackend.mapper.UserInfoMapper;
import com.haonan.ticketsystemmainbackend.service.UserInfoService;
import com.haonan.ticketsystemmainbackend.util.JwtTokenUtil;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户 Service 实现
 */
@Service
@Slf4j
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final JwtTokenUtil jwtTokenUtil;

    private final JwtProperties jwtProperties;

    public UserInfoServiceImpl(JwtTokenUtil jwtTokenUtil, JwtProperties jwtProperties) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.jwtProperties = jwtProperties;
    }

    @Override
    @Transactional
    public UserProfileResponse register(UserRegisterRequest request) {
        UserInfo exists = this.lambdaQuery().eq(UserInfo::getUsername, request.getUsername()).one();
        if (exists != null) {
            throw new BusinessRuntimeException(ResponseCode.USERNAME_ALREADY_EXISTS);
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(UUID.randomUUID().toString().replace("-", ""));
        userInfo.setUsername(request.getUsername());
        userInfo.setPassword(passwordEncoder.encode(request.getPassword()));
        userInfo.setRole(UserConstants.ROLE_USER);
        userInfo.setNickname(StringUtils.defaultIfBlank(request.getNickname(), request.getUsername()));
        userInfo.setAvatar(request.getAvatar());
        userInfo.setEmail(request.getEmail());
        userInfo.setPhone(request.getPhone());
        userInfo.setTicketPreferences(request.getTicketPreferences());

        boolean saved = this.save(userInfo);
        if (!saved) {
            throw new BusinessRuntimeException(ResponseCode.DATABASE_ERROR, "用户注册失败");
        }

        log.info("用户注册成功，userId={}, username={}", userInfo.getUserId(), userInfo.getUsername());
        return toProfileResponse(userInfo);
    }

    @Override
    public UserLoginResponse login(UserLoginRequest request) {
        UserInfo userInfo = this.lambdaQuery().eq(UserInfo::getUsername, request.getUsername()).one();
        if (userInfo == null || !passwordEncoder.matches(request.getPassword(), userInfo.getPassword())) {
            throw new BusinessRuntimeException(ResponseCode.USERNAME_OR_PASSWORD_ERROR);
        }
        if (UserConstants.ROLE_BAN.equals(userInfo.getRole())) {
            throw new BusinessRuntimeException(ResponseCode.USER_BANNED);
        }

        String token = jwtTokenUtil.generateToken(userInfo);
        log.info("用户登录成功，userId={}, username={}", userInfo.getUserId(), userInfo.getUsername());
        return UserLoginResponse.builder()
                .token(token)
                .tokenType(UserConstants.TOKEN_TYPE_BEARER)
                .expiresIn(jwtProperties.getExpireSeconds())
                .userInfo(toProfileResponse(userInfo))
                .build();
    }

    @Override
    public UserProfileResponse getUserProfile(String userId) {
        UserInfo userInfo = this.getById(userId);
        if (userInfo == null) {
            throw new BusinessRuntimeException(ResponseCode.USER_NOT_FOUND);
        }
        return toProfileResponse(userInfo);
    }

    @Override
    @Transactional
    public UserProfileResponse updateUserProfile(String userId, UserUpdateRequest request) {
        UserInfo userInfo = this.getById(userId);
        if (userInfo == null) {
            throw new BusinessRuntimeException(ResponseCode.USER_NOT_FOUND);
        }

        userInfo.setNickname(request.getNickname());
        userInfo.setAvatar(request.getAvatar());
        userInfo.setEmail(request.getEmail());
        userInfo.setPhone(request.getPhone());
        userInfo.setTicketPreferences(request.getTicketPreferences());

        boolean updated = this.updateById(userInfo);
        if (!updated) {
            throw new BusinessRuntimeException(ResponseCode.DATABASE_ERROR, "更新用户信息失败");
        }

        log.info("更新用户信息成功，userId={}", userId);
        return toProfileResponse(userInfo);
    }

    @Override
    public List<UserProfileResponse> listUsers(String username, String role) {
        return this.list(Wrappers.<UserInfo>lambdaQuery()
                        .like(StringUtils.isNotBlank(username), UserInfo::getUsername, username)
                        .eq(StringUtils.isNotBlank(role), UserInfo::getRole, role)
                        .orderByDesc(UserInfo::getCreateTime))
                .stream()
                .map(this::toProfileResponse)
                .collect(Collectors.toList());
    }

    private UserProfileResponse toProfileResponse(UserInfo userInfo) {
        return UserProfileResponse.builder()
                .userId(userInfo.getUserId())
                .username(userInfo.getUsername())
                .role(userInfo.getRole())
                .nickname(userInfo.getNickname())
                .avatar(userInfo.getAvatar())
                .email(userInfo.getEmail())
                .phone(userInfo.getPhone())
                .ticketPreferences(userInfo.getTicketPreferences())
                .build();
    }
}
