package com.haonan.ticketsystemmainbackend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent implements Serializable {
    private String orderId;
    private String userId;

    private String ticketId;
    private int stock_count;
    private long timestamp;

    private static final long serialVersionUID = 1L;
}