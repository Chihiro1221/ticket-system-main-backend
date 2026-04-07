package com.haonan.ticketsystemmainbackend.dto;

import java.math.BigDecimal;
import java.util.Date;
import lombok.Builder;
import lombok.Data;

/**
 * 当前用户订单摘要响应
 */
@Data
@Builder
public class OrderSummaryResponse {

    private String orderId;

    private String userId;

    private String ticketId;

    private String ticketName;

    private BigDecimal amount;

    private Integer status;

    private Date createTime;

    private Date updateTime;

    private Date expireTime;
}
