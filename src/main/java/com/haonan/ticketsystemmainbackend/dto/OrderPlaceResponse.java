package com.haonan.ticketsystemmainbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 下单响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlaceResponse {

    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 票据ID
     */
    private String ticketId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 剩余库存
     */
    private Integer restStock;
}
