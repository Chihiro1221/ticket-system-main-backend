package com.haonan.ticketsystemmainbackend.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 秒杀压测单请求样本
 */
@Data
@Builder
public class SeckillSimulationSample {

    private Integer requestNo;

    private String userId;

    private String ticketId;

    private String outcome;

    private Integer code;

    private String message;

    private Long latencyMs;

    private Long finishedAtMs;
}
