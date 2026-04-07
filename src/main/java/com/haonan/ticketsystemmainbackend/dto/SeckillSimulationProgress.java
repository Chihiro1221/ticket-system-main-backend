package com.haonan.ticketsystemmainbackend.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 秒杀压测流式进度
 */
@Data
@Builder
public class SeckillSimulationProgress {

    private String stage;

    private String ticketId;

    private String ticketName;

    private String scenario;

    private Integer totalRequests;

    private Integer concurrency;

    private Integer completedRequests;

    private Integer successCount;

    private Integer throttledCount;

    private Integer stockInsufficientCount;

    private Integer duplicateCount;

    private Integer otherFailureCount;

    private Long elapsedMs;

    private Double currentQps;

    private Double achievedQps;

    private Double peakQps;

    private Long avgLatencyMs;

    private Long p95LatencyMs;

    private Integer restStock;

    private List<SeckillSimulationSlice> slices;

    private List<SeckillSimulationSample> samples;
}
