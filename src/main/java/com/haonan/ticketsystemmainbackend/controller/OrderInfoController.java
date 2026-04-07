package com.haonan.ticketsystemmainbackend.controller;

import com.haonan.ticketsystemmainbackend.common.Result;
import com.haonan.ticketsystemmainbackend.dto.OrderSummaryResponse;
import com.haonan.ticketsystemmainbackend.service.OrderInfoService;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单控制器
 */
@RestController
@RequestMapping("/api/orders")
public class OrderInfoController {

    @Resource
    private OrderInfoService orderInfoService;

    @GetMapping("/current")
    public Result<List<OrderSummaryResponse>> listCurrentUserOrders(@RequestHeader("x-user-id") String userId) {
        return Result.success(orderInfoService.listCurrentUserOrders(userId));
    }
}
