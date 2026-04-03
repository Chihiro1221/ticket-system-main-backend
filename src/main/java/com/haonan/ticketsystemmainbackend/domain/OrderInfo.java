package com.haonan.ticketsystemmainbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单表
 * @TableName order_info
 */
@TableName(value ="order_info")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderInfo {
    /**
     * 订单ID
     */
    @TableId
    private String orderId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 车次或场次ID
     */
    private String ticketId;

    /**
     * 状态: 0-待支付, 1-已支付
     */
    private Integer status;

    /**
     * 下单时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}