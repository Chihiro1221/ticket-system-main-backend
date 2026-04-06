package com.haonan.ticketsystemmainbackend.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 用户基础信息表
 */
@TableName(value = "user_info")
@Data
public class UserInfo {

    /**
     * 用户ID
     */
    @TableId
    private String userId;

    /**
     * 账号名
     */
    private String username;

    /**
     * 密码（加密后）
     */
    private String password;

    /**
     * 角色：admin/user/ban
     */
    private String role;

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
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 票务偏好信息，预留扩展
     */
    private String ticketPreferences;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
