package com.haonan.ticketsystemmainbackend.controller;

import com.haonan.ticketsystemmainbackend.actor.TicketActor;
import com.haonan.ticketsystemmainbackend.common.Result;
import com.haonan.ticketsystemmainbackend.common.ResponseCode;
import com.haonan.ticketsystemmainbackend.common.constants.DaprConstants;
import com.haonan.ticketsystemmainbackend.common.constants.OrderConstants;
import com.haonan.ticketsystemmainbackend.common.constants.SystemConstants;
import com.haonan.ticketsystemmainbackend.dapr.DaprClientHolder;
import com.haonan.ticketsystemmainbackend.domain.OrderInfo;
import com.haonan.ticketsystemmainbackend.domain.TicketOrderCommand;
import com.haonan.ticketsystemmainbackend.dto.OrderRequest;
import com.haonan.ticketsystemmainbackend.dto.OrderPlaceResponse;
import com.haonan.ticketsystemmainbackend.exception.BusinessRuntimeException;
import com.haonan.ticketsystemmainbackend.service.OrderInfoService;
import com.haonan.ticketsystemmainbackend.util.UserLock;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import io.dapr.actors.ActorId;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.client.domain.SaveStateRequest;
import io.dapr.client.domain.State;
import io.dapr.client.domain.StateOptions;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

/**
 * 票务控制器
 * 提供票务相关的接口，包括下单、查询等
 *
 * @author heart
 */
@RestController
@RequestMapping("/api/ticket-stock")
@Slf4j
public class TicketStockController {

    @Resource
    private UserLock userLock;

    @Resource
    private OrderInfoService orderInfoService;

    /**
     * 下单接口
     * 保证同一用户不能重复下单（通过分布式锁 + 数据库唯一索引）
     *
     * @param request 下单请求
     * @return 下单响应
     */
    @PostMapping("/order")
    public Result<OrderPlaceResponse> placeOrder(@Valid @RequestBody OrderRequest request) {
        String userId = request.getUserId();
        String ticketId = request.getTicketId();
        Integer count = request.getCount();

        log.info("收到下单请求 - 用户ID: {}, 票务ID: {}, 数量: {}", userId, ticketId, count);

        if (!Integer.valueOf(SystemConstants.STOCK_DEDUCT_COUNT).equals(count)) {
            throw new BusinessRuntimeException(ResponseCode.BAD_REQUEST, "当前场景仅支持单用户单次购买1张票");
        }

        // 使用分布式锁防止同一用户并发下单
        return userLock.executeWithLock(userId, () -> {
            // 检查限购记录
            String recordKey = String.format(DaprConstants.KEY_PURCHASE_RECORD_FORMAT, userId, ticketId);
            State<String> record = DaprClientHolder.getClient().getState(DaprConstants.STATE_STORE_NAME, recordKey, String.class).block();

            if (record != null && record.getValue() != null) {
                // 如果状态是 "PAID" 或 "PENDING"，则不允许再买
                throw new BusinessRuntimeException(ResponseCode.USER_ALREADY_PURCHASED);
            }

            String orderId = IdWorker.getIdStr();
            Map<String, String> metadata = Collections.singletonMap("ttlInSeconds", String.valueOf(SystemConstants.STATE_EXPIRY_SECONDS));
            StateOptions stateOptions = new StateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.FIRST_WRITE);
            State<String> stateRecord = new State<>(
                    recordKey,
                    OrderConstants.ORDER_STATUS_STR_PENDING,
                    null,
                    metadata,
                    stateOptions
            );
            DaprClientHolder.getClient().saveBulkState(new SaveStateRequest(DaprConstants.STATE_STORE_NAME).setStates(stateRecord)).block();

            // 创建 Actor 代理
            TicketActor ticketActor = new ActorProxyBuilder<>(TicketActor.class, DaprClientHolder.getActorClient())
                    .build(new ActorId(ticketId));

            // 调用 Actor 扣减库存
            Boolean success;
            try {
                success = ticketActor.deductTicket(TicketOrderCommand.builder()
                        .orderId(orderId)
                        .userId(userId)
                        .ticketId(ticketId)
                        .count(count)
                        .build()).block();
            } catch (Exception e) {
                DaprClientHolder.getClient().deleteState(DaprConstants.STATE_STORE_NAME, recordKey).block();
                throw e;
            }

            if (Boolean.TRUE.equals(success)) {
                // 获取剩余库存
                Integer restStock = ticketActor.getRestCount().block();

                log.info("下单成功 - 用户ID: {}, 票务ID: {}, 订单ID: {}, 剩余库存: {}", userId, ticketId, orderId, restStock);
                return Result.success(OrderConstants.MSG_ORDER_SUCCESS, OrderPlaceResponse.builder()
                        .orderId(orderId)
                        .ticketId(ticketId)
                        .userId(userId)
                        .restStock(restStock)
                        .build());
            } else {
                DaprClientHolder.getClient().deleteState(DaprConstants.STATE_STORE_NAME, recordKey).block();
                log.warn("下单失败 - 库存不足，用户ID: {}, 票务ID: {}", userId, ticketId);
                throw new BusinessRuntimeException(ResponseCode.STOCK_INSUFFICIENT);
            }
        });
    }

    /**
     * 查询票务剩余库存
     *
     * @param ticketId 票务ID
     * @return 剩余库存
     */
    @GetMapping("/stock/{ticketId}")
    public Result<Integer> getStock(@PathVariable String ticketId) {
        TicketActor ticketActor = new ActorProxyBuilder<>(TicketActor.class, DaprClientHolder.getActorClient())
                .build(new ActorId(ticketId));

        Integer restStock = ticketActor.getRestCount().block();
        log.info("查询库存 - 票务ID: {}, 剩余库存: {}", ticketId, restStock);

        return Result.success(restStock);
    }

    /**
     * 确认支付
     *
     * @return 支付确认结果
     */
    @PostMapping("/payment/confirm")
    public Result<String> confirmPayment(@RequestBody OrderInfo orderInfoReq) {
        String orderId = orderInfoReq.getOrderId();
        if (StringUtils.isBlank(orderId)) {
            throw new BusinessRuntimeException(ResponseCode.BAD_REQUEST);
        }
        OrderInfo orderInfo = orderInfoService.lambdaQuery().eq(OrderInfo::getOrderId, orderId).one();
        if (orderInfo == null) {
            throw new BusinessRuntimeException(ResponseCode.ORDER_NOT_FOUND);
        }
        if (orderInfo.getStatus() == OrderConstants.ORDER_STATUS_PAID) {
            throw new BusinessRuntimeException(ResponseCode.ORDER_ALREADY_PAID);
        }
        // 已取消
        if (orderInfo.getStatus() == OrderConstants.ORDER_STATUS_CANCELLED) {
            throw new BusinessRuntimeException(ResponseCode.ORDER_ALREADY_CANCELLED);
        }
        TicketActor ticketActor = new ActorProxyBuilder<>(TicketActor.class, DaprClientHolder.getActorClient())
                .build(new ActorId(orderInfo.getTicketId()));

        ticketActor.confirmPayment(orderInfo).block();
        log.info("支付确认成功 - 订单ID: {}, 票务ID: {}", orderId, orderInfo.getTicketId());

        return Result.success("支付确认成功", orderId);
    }
}
