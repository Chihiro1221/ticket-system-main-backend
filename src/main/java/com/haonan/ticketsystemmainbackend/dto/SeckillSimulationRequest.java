package com.haonan.ticketsystemmainbackend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 秒杀压测请求
 */
@Data
public class SeckillSimulationRequest {

    @NotBlank(message = "票务ID不能为空")
    private String ticketId;

    @NotNull(message = "总请求数不能为空")
    @Min(value = 10, message = "总请求数不能少于10")
    @Max(value = 2000, message = "总请求数不能超过2000")
    private Integer totalRequests;

    @NotNull(message = "并发数不能为空")
    @Min(value = 1, message = "并发数不能小于1")
    @Max(value = 300, message = "并发数不能超过300")
    private Integer concurrency;

    /**
     * 压测场景：
     * SAME_TICKET_DIFF_USERS - 同票不同用户
     * SAME_TICKET_SAME_USER - 同票同用户
     * MULTI_TICKET_DIFF_USERS - 多票不同用户
     */
    private String scenario;
}
