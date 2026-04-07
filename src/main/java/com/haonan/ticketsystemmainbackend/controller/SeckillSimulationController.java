package com.haonan.ticketsystemmainbackend.controller;

import com.haonan.ticketsystemmainbackend.common.Result;
import com.haonan.ticketsystemmainbackend.dto.SeckillSimulationRequest;
import com.haonan.ticketsystemmainbackend.dto.SeckillSimulationResponse;
import com.haonan.ticketsystemmainbackend.service.SeckillSimulationService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 秒杀压测演示控制器
 */
@RestController
@RequestMapping("/api/demo/seckill")
public class SeckillSimulationController {

    @Resource
    private SeckillSimulationService seckillSimulationService;

    @PostMapping("/run")
    public Result<SeckillSimulationResponse> run(@Valid @RequestBody SeckillSimulationRequest request) {
        return Result.success(seckillSimulationService.runSimulation(request));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestParam(required = false) String ticketId,
            @RequestParam(required = false) Integer totalRequests,
            @RequestParam(required = false) Integer concurrency,
            @RequestParam(required = false) String scenario
    ) {
        SseEmitter emitter = new SseEmitter(0L);
        SeckillSimulationRequest request = new SeckillSimulationRequest();
        request.setTicketId(ticketId);
        request.setTotalRequests(totalRequests);
        request.setConcurrency(concurrency);
        request.setScenario(scenario);

        CompletableFuture.runAsync(() -> {
            try {
                String validationMessage = validateStreamRequest(request);
                if (validationMessage != null) {
                    emitter.send(SseEmitter.event().name("error").data(validationMessage));
                    emitter.complete();
                    return;
                }

                seckillSimulationService.streamSimulation(request, progress -> {
                    try {
                        emitter.send(SseEmitter.event().name("progress").data(progress));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (IOException ignored) {
                    // ignore
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private String validateStreamRequest(SeckillSimulationRequest request) {
        if (request.getTicketId() == null || request.getTicketId().isBlank()) {
            return "票务ID不能为空";
        }
        if (request.getTotalRequests() == null) {
            return "总请求数不能为空";
        }
        if (request.getTotalRequests() < 10 || request.getTotalRequests() > 2000) {
            return "总请求数需在 10 到 2000 之间";
        }
        if (request.getConcurrency() == null) {
            return "并发数不能为空";
        }
        if (request.getConcurrency() < 1 || request.getConcurrency() > 300) {
            return "并发数需在 1 到 300 之间";
        }
        return null;
    }
}
