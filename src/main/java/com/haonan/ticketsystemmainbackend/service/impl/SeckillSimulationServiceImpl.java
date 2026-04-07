package com.haonan.ticketsystemmainbackend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haonan.ticketsystemmainbackend.common.ResponseCode;
import com.haonan.ticketsystemmainbackend.domain.TicketInfo;
import com.haonan.ticketsystemmainbackend.domain.UserInfo;
import com.haonan.ticketsystemmainbackend.dto.SeckillSimulationProgress;
import com.haonan.ticketsystemmainbackend.dto.SeckillSimulationRequest;
import com.haonan.ticketsystemmainbackend.dto.SeckillSimulationResponse;
import com.haonan.ticketsystemmainbackend.dto.SeckillSimulationSample;
import com.haonan.ticketsystemmainbackend.dto.SeckillSimulationSlice;
import com.haonan.ticketsystemmainbackend.exception.BusinessRuntimeException;
import com.haonan.ticketsystemmainbackend.service.SeckillSimulationService;
import com.haonan.ticketsystemmainbackend.service.TicketInfoService;
import com.haonan.ticketsystemmainbackend.util.JwtTokenUtil;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

@Service
public class SeckillSimulationServiceImpl implements SeckillSimulationService {

    private static final String GATEWAY_ORDER_URL = "http://localhost:8081/ticket-service/api/ticket-stock/order";
    private static final int SLICE_SIZE_MS = 200;
    @Resource
    private TicketInfoService ticketInfoService;

    @Resource
    private JwtTokenUtil jwtTokenUtil;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public SeckillSimulationResponse runSimulation(SeckillSimulationRequest request) {
        SimulationContext context = prepareContext(request);
        List<SimulationResult> results = execute(context, null);
        return buildResponse(context, results);
    }

    @Override
    public void streamSimulation(SeckillSimulationRequest request, Consumer<SeckillSimulationProgress> progressConsumer) {
        SimulationContext context = prepareContext(request);
        List<SimulationResult> results = execute(context, progressConsumer);
        progressConsumer.accept(buildProgress(context, results, "complete"));
    }

    private SimulationContext prepareContext(SeckillSimulationRequest request) {
        TicketInfo primaryTicket = ticketInfoService.getById(request.getTicketId());
        if (primaryTicket == null) {
            throw new BusinessRuntimeException(ResponseCode.TICKET_NOT_FOUND);
        }

        List<String> ticketIds = resolveTicketIds(request.getTicketId());
        String scenario = normalizeScenario(request.getScenario());
        String token = generateAdminToken();
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        return new SimulationContext(request, primaryTicket, ticketIds, scenario, token, httpClient);
    }

    private List<SimulationResult> execute(
            SimulationContext context,
            Consumer<SeckillSimulationProgress> progressConsumer
    ) {
        ExecutorService executorService = Executors.newFixedThreadPool(context.request().getConcurrency());
        CompletionService<SimulationResult> completionService = new ExecutorCompletionService<>(executorService);
        long globalStart = System.nanoTime();

        for (int i = 0; i < context.request().getTotalRequests(); i++) {
            int requestNo = i + 1;
            completionService.submit(createTask(context, requestNo, globalStart));
        }

        List<SimulationResult> results = new ArrayList<>();
        try {
            for (int i = 0; i < context.request().getTotalRequests(); i++) {
                SimulationResult result = completionService.take().get();
                results.add(result);

                if (progressConsumer != null && shouldEmitProgress(i + 1, context.request().getTotalRequests())) {
                    progressConsumer.accept(buildProgress(context, results, "progress"));
                }
            }
        } catch (Exception e) {
            throw new BusinessRuntimeException(ResponseCode.INTERNAL_SERVER_ERROR, "压测执行失败: " + e.getMessage());
        } finally {
            executorService.shutdown();
            try {
                executorService.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }

        results.sort(Comparator.comparingLong(SimulationResult::finishedAtMs));
        return results;
    }

    private boolean shouldEmitProgress(int completedRequests, int totalRequests) {
        return completedRequests == 1
                || completedRequests == totalRequests
                || completedRequests % 5 == 0;
    }

    private Callable<SimulationResult> createTask(SimulationContext context, int requestNo, long globalStart) {
        return () -> {
            String ticketId = resolveTicketId(context, requestNo);
            String userId = resolveUserId(context, requestNo, ticketId);
            long start = System.nanoTime();

            try {
                String body = objectMapper.writeValueAsString(new OrderPayload(userId, ticketId, 1));
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(GATEWAY_ORDER_URL))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + context.token())
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();

                HttpResponse<String> response = context.httpClient().send(request, HttpResponse.BodyHandlers.ofString());
                long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                long finishedAtMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - globalStart);

                if (response.statusCode() == 429) {
                    return new SimulationResult(
                            requestNo,
                            userId,
                            ticketId,
                            "THROTTLED",
                            429,
                            "网关限流",
                            latencyMs,
                            finishedAtMs
                    );
                }

                JsonNode root = objectMapper.readTree(response.body());
                int code = root.path("code").asInt(response.statusCode());
                String message = root.path("message").asText();

                if (code == ResponseCode.SUCCESS.getCode()) {
                    return new SimulationResult(requestNo, userId, ticketId, "SUCCESS", code, message, latencyMs, finishedAtMs);
                }
                if (code == ResponseCode.STOCK_INSUFFICIENT.getCode()) {
                    return new SimulationResult(
                            requestNo,
                            userId,
                            ticketId,
                            "STOCK_INSUFFICIENT",
                            code,
                            message,
                            latencyMs,
                            finishedAtMs
                    );
                }
                if (code == ResponseCode.USER_ALREADY_PURCHASED.getCode()
                        || code == ResponseCode.USER_OPERATION_TOO_FREQUENT.getCode()) {
                    return new SimulationResult(requestNo, userId, ticketId, "DUPLICATE", code, message, latencyMs, finishedAtMs);
                }
                if (code == ResponseCode.TOO_MANY_REQUESTS.getCode()) {
                    return new SimulationResult(requestNo, userId, ticketId, "THROTTLED", code, message, latencyMs, finishedAtMs);
                }

                return new SimulationResult(requestNo, userId, ticketId, "FAILED", code, message, latencyMs, finishedAtMs);
            } catch (Exception e) {
                long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                long finishedAtMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - globalStart);
                return new SimulationResult(requestNo, userId, ticketId, "FAILED", 500, e.getMessage(), latencyMs, finishedAtMs);
            }
        };
    }

    private SeckillSimulationResponse buildResponse(SimulationContext context, List<SimulationResult> results) {
        Summary summary = summarize(context, results);

        return SeckillSimulationResponse.builder()
                .ticketId(context.primaryTicket().getTicketId())
                .ticketName(context.primaryTicket().getName())
                .totalRequests(context.request().getTotalRequests())
                .concurrency(context.request().getConcurrency())
                .scenario(context.scenario())
                .successCount(summary.successCount())
                .throttledCount(summary.throttledCount())
                .stockInsufficientCount(summary.stockInsufficientCount())
                .duplicateCount(summary.duplicateCount())
                .otherFailureCount(summary.otherFailureCount())
                .totalDurationMs(summary.elapsedMs())
                .achievedQps(summary.achievedQps())
                .avgLatencyMs(summary.avgLatencyMs())
                .p95LatencyMs(summary.p95LatencyMs())
                .restStock(summary.restStock())
                .slices(summary.slices())
                .samples(summary.samples())
                .build();
    }

    private SeckillSimulationProgress buildProgress(
            SimulationContext context,
            List<SimulationResult> partialResults,
            String stage
    ) {
        Summary summary = summarize(context, partialResults);

        return SeckillSimulationProgress.builder()
                .stage(stage)
                .ticketId(context.primaryTicket().getTicketId())
                .ticketName(context.primaryTicket().getName())
                .scenario(context.scenario())
                .totalRequests(context.request().getTotalRequests())
                .concurrency(context.request().getConcurrency())
                .completedRequests(partialResults.size())
                .successCount(summary.successCount())
                .throttledCount(summary.throttledCount())
                .stockInsufficientCount(summary.stockInsufficientCount())
                .duplicateCount(summary.duplicateCount())
                .otherFailureCount(summary.otherFailureCount())
                .elapsedMs(summary.elapsedMs())
                .currentQps(summary.currentQps())
                .achievedQps(summary.achievedQps())
                .peakQps(summary.peakQps())
                .avgLatencyMs(summary.avgLatencyMs())
                .p95LatencyMs(summary.p95LatencyMs())
                .restStock(summary.restStock())
                .slices(summary.slices())
                .samples(summary.samples())
                .build();
    }

    private Summary summarize(SimulationContext context, List<SimulationResult> sourceResults) {
        List<SimulationResult> results = sourceResults.stream()
                .sorted(Comparator.comparingLong(SimulationResult::finishedAtMs))
                .toList();

        long elapsedMs = results.stream().mapToLong(SimulationResult::finishedAtMs).max().orElse(0L);
        List<Long> latencies = results.stream().map(SimulationResult::latencyMs).sorted().toList();
        long avgLatency = results.isEmpty() ? 0 : Math.round(latencies.stream().mapToLong(Long::longValue).average().orElse(0));
        long p95Latency = latencies.isEmpty() ? 0 : latencies.get(Math.min((int) Math.ceil(latencies.size() * 0.95) - 1, latencies.size() - 1));

        int successCount = (int) results.stream().filter(result -> "SUCCESS".equals(result.outcome())).count();
        int throttledCount = (int) results.stream().filter(result -> "THROTTLED".equals(result.outcome())).count();
        int stockInsufficientCount = (int) results.stream().filter(result -> "STOCK_INSUFFICIENT".equals(result.outcome())).count();
        int duplicateCount = (int) results.stream().filter(result -> "DUPLICATE".equals(result.outcome())).count();
        int otherFailureCount = results.size() - successCount - throttledCount - stockInsufficientCount - duplicateCount;

        List<SeckillSimulationSlice> slices = buildSlices(results);
        double peakQps = slices.stream()
                .mapToDouble(slice -> round(slice.getTotal() * 1000D / SLICE_SIZE_MS))
                .max()
                .orElse(0D);
        double currentQps = slices.isEmpty()
                ? 0D
                : round(slices.get(slices.size() - 1).getTotal() * 1000D / SLICE_SIZE_MS);
        double achievedQps = elapsedMs == 0 ? 0D : round(results.size() * 1000D / elapsedMs);

        List<SeckillSimulationSample> samples = results.stream()
                .sorted(Comparator.comparingLong(SimulationResult::finishedAtMs).reversed())
                .map(result -> SeckillSimulationSample.builder()
                        .requestNo(result.requestNo())
                        .userId(result.userId())
                        .ticketId(result.ticketId())
                        .outcome(result.outcome())
                        .code(result.code())
                        .message(result.message())
                        .latencyMs(result.latencyMs())
                        .finishedAtMs(result.finishedAtMs())
                        .build())
                .toList();

        int restStock = ticketInfoService.getTicketDetail(context.primaryTicket().getTicketId()).getRemainingStock();

        return new Summary(
                successCount,
                throttledCount,
                stockInsufficientCount,
                duplicateCount,
                otherFailureCount,
                elapsedMs,
                currentQps,
                achievedQps,
                peakQps,
                avgLatency,
                p95Latency,
                restStock,
                slices,
                samples
        );
    }

    private List<SeckillSimulationSlice> buildSlices(List<SimulationResult> results) {
        if (results.isEmpty()) {
            return List.of();
        }

        long maxTime = results.stream().mapToLong(SimulationResult::finishedAtMs).max().orElse(0L);
        int sliceCount = (int) Math.ceil((maxTime + 1D) / SLICE_SIZE_MS);
        List<SeckillSimulationSlice> slices = new ArrayList<>();

        for (int i = 0; i < sliceCount; i++) {
            long start = (long) i * SLICE_SIZE_MS;
            long end = start + SLICE_SIZE_MS;
            List<SimulationResult> bucket = results.stream()
                    .filter(result -> result.finishedAtMs() >= start && result.finishedAtMs() < end)
                    .toList();

            slices.add(SeckillSimulationSlice.builder()
                    .sliceIndex(i + 1)
                    .startMs(start)
                    .endMs(end)
                    .total(bucket.size())
                    .success((int) bucket.stream().filter(result -> "SUCCESS".equals(result.outcome())).count())
                    .throttled((int) bucket.stream().filter(result -> "THROTTLED".equals(result.outcome())).count())
                    .stockInsufficient((int) bucket.stream().filter(result -> "STOCK_INSUFFICIENT".equals(result.outcome())).count())
                    .duplicate((int) bucket.stream().filter(result -> "DUPLICATE".equals(result.outcome())).count())
                    .otherFailure((int) bucket.stream().filter(result -> "FAILED".equals(result.outcome())).count())
                    .build());
        }

        return slices;
    }

    private List<String> resolveTicketIds(String primaryTicketId) {
        List<String> ticketIds = new ArrayList<>();
        ticketIds.add(primaryTicketId);

        ticketInfoService.list().stream()
                .map(TicketInfo::getTicketId)
                .filter(ticketId -> !primaryTicketId.equals(ticketId))
                .limit(2)
                .forEach(ticketIds::add);

        return ticketIds;
    }

    private String resolveTicketId(SimulationContext context, int requestNo) {
        if (!"MULTI_TICKET_DIFF_USERS".equals(context.scenario())) {
            return context.primaryTicket().getTicketId();
        }
        return context.ticketIds().get((requestNo - 1) % context.ticketIds().size());
    }

    private String resolveUserId(SimulationContext context, int requestNo, String ticketId) {
        if ("SAME_TICKET_SAME_USER".equals(context.scenario())) {
            return "sim-repeat-user-" + ticketId;
        }
        return "sim-user-" + ticketId + "-" + requestNo + "-" + System.nanoTime();
    }

    private String normalizeScenario(String scenario) {
        if ("SAME_TICKET_SAME_USER".equalsIgnoreCase(scenario)) {
            return "SAME_TICKET_SAME_USER";
        }
        if ("MULTI_TICKET_DIFF_USERS".equalsIgnoreCase(scenario)) {
            return "MULTI_TICKET_DIFF_USERS";
        }
        return "SAME_TICKET_DIFF_USERS";
    }

    private String generateAdminToken() {
        UserInfo adminUser = new UserInfo();
        adminUser.setUserId("admin-simulator");
        adminUser.setUsername("admin");
        adminUser.setRole("admin");
        return jwtTokenUtil.generateToken(adminUser);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
    }

    private record OrderPayload(String userId, String ticketId, Integer count) {
    }

    private record SimulationContext(
            SeckillSimulationRequest request,
            TicketInfo primaryTicket,
            List<String> ticketIds,
            String scenario,
            String token,
            HttpClient httpClient
    ) {
    }

    private record SimulationResult(
            Integer requestNo,
            String userId,
            String ticketId,
            String outcome,
            Integer code,
            String message,
            Long latencyMs,
            Long finishedAtMs
    ) {
    }

    private record Summary(
            Integer successCount,
            Integer throttledCount,
            Integer stockInsufficientCount,
            Integer duplicateCount,
            Integer otherFailureCount,
            Long elapsedMs,
            Double currentQps,
            Double achievedQps,
            Double peakQps,
            Long avgLatencyMs,
            Long p95LatencyMs,
            Integer restStock,
            List<SeckillSimulationSlice> slices,
            List<SeckillSimulationSample> samples
    ) {
    }
}
