package com.haonan.ticketsystemmainbackend.controller;

import com.haonan.ticketsystemmainbackend.common.Result;
import com.haonan.ticketsystemmainbackend.domain.TicketInfo;
import com.haonan.ticketsystemmainbackend.dto.TicketCreateRequest;
import com.haonan.ticketsystemmainbackend.dto.TicketInventoryResponse;
import com.haonan.ticketsystemmainbackend.dto.TicketUpdateRequest;
import com.haonan.ticketsystemmainbackend.service.TicketInfoService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 票据基础信息控制器
 */
@RestController
@RequestMapping("/api/ticket")
public class TicketInfoController {

    @Resource
    private TicketInfoService ticketInfoService;

    @PostMapping
    public Result<TicketInfo> createTicket(@Valid @RequestBody TicketCreateRequest request) {
        return Result.success("新增票据成功", ticketInfoService.createTicket(request));
    }

    @PutMapping("/{ticketId}")
    public Result<TicketInfo> updateTicket(@PathVariable String ticketId,
                                           @Valid @RequestBody TicketUpdateRequest request) {
        return Result.success("更新票据成功", ticketInfoService.updateTicket(ticketId, request));
    }

    @DeleteMapping("/{ticketId}")
    public Result<Void> deleteTicket(@PathVariable String ticketId) {
        ticketInfoService.deleteTicket(ticketId);
        return Result.success("删除票据成功");
    }

    @GetMapping("/{ticketId}")
    public Result<TicketInventoryResponse> getTicketDetail(@PathVariable String ticketId) {
        return Result.success(ticketInfoService.getTicketDetail(ticketId));
    }

    @GetMapping
    public Result<List<TicketInventoryResponse>> listTickets(@RequestParam(required = false) String name,
                                                             @RequestParam(required = false) Integer status) {
        return Result.success(ticketInfoService.listTickets(name, status));
    }
}
