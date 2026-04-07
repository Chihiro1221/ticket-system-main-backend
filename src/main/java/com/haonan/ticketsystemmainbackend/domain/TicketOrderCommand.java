package com.haonan.ticketsystemmainbackend.domain;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 票据下单命令
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketOrderCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 票据ID
     */
    private String ticketId;

    /**
     * 购买数量，当前简化为固定 1
     */
    private Integer count;

    /**
     * 控制器发起 Actor 调用的时间戳，用于估算进入 Actor 前的等待时间
     */
    private Long actorInvokeAtMs;
}
