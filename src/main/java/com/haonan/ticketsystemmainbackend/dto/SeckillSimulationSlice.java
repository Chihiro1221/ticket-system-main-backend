package com.haonan.ticketsystemmainbackend.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 秒杀压测时间片结果
 */
@Data
@Builder
public class SeckillSimulationSlice {

    private Integer sliceIndex;

    private Long startMs;

    private Long endMs;

    private Integer total;

    private Integer success;

    private Integer throttled;

    private Integer stockInsufficient;

    private Integer duplicate;

    private Integer otherFailure;
}
