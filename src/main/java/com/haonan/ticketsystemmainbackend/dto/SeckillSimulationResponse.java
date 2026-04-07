package com.haonan.ticketsystemmainbackend.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 秒杀压测响应
 */
@Data
@Builder
public class SeckillSimulationResponse {

    private String ticketId;

    private String ticketName;

    private Integer totalRequests;

    private Integer concurrency;

    private String scenario;

    private Integer successCount;

    private Integer throttledCount;

    private Integer stockInsufficientCount;

    private Integer duplicateCount;

    private Integer otherFailureCount;

    private Long totalDurationMs;

    private Double achievedQps;

    private Long avgLatencyMs;

    private Long p95LatencyMs;

    private Integer restStock;

    private List<SeckillSimulationSlice> slices;

    private List<SeckillSimulationSample> samples;
}
