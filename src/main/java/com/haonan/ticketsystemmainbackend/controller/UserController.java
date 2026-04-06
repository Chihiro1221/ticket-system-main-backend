package com.haonan.ticketsystemmainbackend.controller;

import com.haonan.ticketsystemmainbackend.common.Result;
import com.haonan.ticketsystemmainbackend.dto.UserLoginRequest;
import com.haonan.ticketsystemmainbackend.dto.UserLoginResponse;
import com.haonan.ticketsystemmainbackend.dto.UserProfileResponse;
import com.haonan.ticketsystemmainbackend.dto.UserRegisterRequest;
import com.haonan.ticketsystemmainbackend.dto.UserUpdateRequest;
import com.haonan.ticketsystemmainbackend.service.UserInfoService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Resource
    private UserInfoService userInfoService;

    @PostMapping("/register")
    public Result<UserProfileResponse> register(@Valid @RequestBody UserRegisterRequest request) {
        return Result.success("注册成功", userInfoService.register(request));
    }

    @PostMapping("/login")
    public Result<UserLoginResponse> login(@Valid @RequestBody UserLoginRequest request) {
        return Result.success("登录成功", userInfoService.login(request));
    }

    @GetMapping("/{userId}")
    public Result<UserProfileResponse> getUserProfile(@PathVariable String userId) {
        return Result.success(userInfoService.getUserProfile(userId));
    }

    @PutMapping("/{userId}")
    public Result<UserProfileResponse> updateUserProfile(@PathVariable String userId,
                                                         @Valid @RequestBody UserUpdateRequest request) {
        return Result.success("更新用户信息成功", userInfoService.updateUserProfile(userId, request));
    }

    @GetMapping
    public Result<List<UserProfileResponse>> listUsers(@RequestParam(required = false) String username,
                                                       @RequestParam(required = false) String role) {
        return Result.success(userInfoService.listUsers(username, role));
    }
}
