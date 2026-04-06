package com.haonan.ticketsystemmainbackend.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 票据基础信息表
 */
@TableName(value = "ticket_info")
@Data
public class TicketInfo {

    /**
     * 票据ID
     */
    @TableId
    private String ticketId;

    /**
     * 票据名称
     */
    private String name;

    /**
     * 票据描述
     */
    private String description;

    /**
     * 票价
     */
    private BigDecimal price;

    /**
     * 票据状态: 0-下架, 1-上架
     */
    private Integer status;

    /**
     * 场次时间
     */
    private Date eventTime;

    /**
     * 开售时间
     */
    private Date saleStartTime;

    /**
     * 截止售卖时间
     */
    private Date saleEndTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
