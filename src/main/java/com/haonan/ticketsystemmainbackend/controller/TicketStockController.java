package com.haonan.ticketsystemmainbackend.controller;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.haonan.ticketsystemmainbackend.actor.TicketActor;
import com.haonan.ticketsystemmainbackend.common.ResponseCode;
import com.haonan.ticketsystemmainbackend.common.Result;
import com.haonan.ticketsystemmainbackend.common.constants.DaprConstants;
import com.haonan.ticketsystemmainbackend.common.constants.OrderConstants;
import com.haonan.ticketsystemmainbackend.common.constants.SystemConstants;
import com.haonan.ticketsystemmainbackend.dapr.DaprClientHolder;
import com.haonan.ticketsystemmainbackend.domain.OrderInfo;
import com.haonan.ticketsystemmainbackend.domain.TicketOrderCommand;
import com.haonan.ticketsystemmainbackend.dto.OrderPlaceResponse;
import com.haonan.ticketsystemmainbackend.dto.OrderRequest;
import com.haonan.ticketsystemmainbackend.exception.BusinessRuntimeException;
import com.haonan.ticketsystemmainbackend.service.OrderInfoService;
import com.haonan.ticketsystemmainbackend.util.UserLock;
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
     */
    @PostMapping("/order")
    public Result<OrderPlaceResponse> placeOrder(@Valid @RequestBody OrderRequest request) {
        String userId = request.getUserId();
        String ticketId = request.getTicketId();
        Integer count = request.getCount();
        long requestStart = System.nanoTime();
        long[] phaseCosts = new long[4];
        String[] orderIdHolder = new String[1];
        UserLock.LockExecutionResult<Result<OrderPlaceResponse>> lockResult;

        if (!Integer.valueOf(SystemConstants.STOCK_DEDUCT_COUNT).equals(count)) {
            throw new BusinessRuntimeException(ResponseCode.BAD_REQUEST, "当前场景仅支持单用户单次购买1张票");
        }

        try {
            lockResult = userLock.executeWithLockMetrics(userId, () -> {
                String recordKey = String.format(DaprConstants.KEY_PURCHASE_RECORD_FORMAT, userId, ticketId);

                long checkRecordStart = System.nanoTime();
                State<String> record = DaprClientHolder.getClient()
                        .getState(DaprConstants.STATE_STORE_NAME, recordKey, String.class)
                        .block();
                phaseCosts[0] = elapsedMs(checkRecordStart);

                if (record != null && record.getValue() != null) {
                    throw new BusinessRuntimeException(ResponseCode.USER_ALREADY_PURCHASED);
                }

                String orderId = IdWorker.getIdStr();
                orderIdHolder[0] = orderId;

                Map<String, String> metadata = Collections.singletonMap("ttlInSeconds", String.valueOf(SystemConstants.STATE_EXPIRY_SECONDS));
                StateOptions stateOptions = new StateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.FIRST_WRITE);
                State<String> stateRecord = new State<>(
                        recordKey,
                        OrderConstants.ORDER_STATUS_STR_PENDING,
                        null,
                        metadata,
                        stateOptions
                );

                long saveRecordStart = System.nanoTime();
                DaprClientHolder.getClient()
                        .saveBulkState(new SaveStateRequest(DaprConstants.STATE_STORE_NAME).setStates(stateRecord))
                        .block();
                phaseCosts[1] = elapsedMs(saveRecordStart);

                TicketActor ticketActor = new ActorProxyBuilder<>(TicketActor.class, DaprClientHolder.getActorClient())
                        .build(new ActorId(ticketId));

                Integer restStock;
                long actorDeductStart = System.nanoTime();
                try {
                    restStock = ticketActor.deductTicket(TicketOrderCommand.builder()
                            .orderId(orderId)
                            .userId(userId)
                            .ticketId(ticketId)
                            .count(count)
                            .actorInvokeAtMs(System.currentTimeMillis())
                            .build()).block();
                } catch (Exception e) {
                    DaprClientHolder.getClient().deleteState(DaprConstants.STATE_STORE_NAME, recordKey).block();
                    log.error("下单链路异常，已清理购买记录 orderId={} userId={} ticketId={}", orderId, userId, ticketId, e);
                    throw e;
                }
                phaseCosts[2] = elapsedMs(actorDeductStart);

                if (restStock != null) {
                    phaseCosts[3] = 0L;
                    return Result.success(OrderConstants.MSG_ORDER_SUCCESS, OrderPlaceResponse.builder()
                            .orderId(orderId)
                            .ticketId(ticketId)
                            .userId(userId)
                            .restStock(restStock)
                            .build());
                }

                DaprClientHolder.getClient().deleteState(DaprConstants.STATE_STORE_NAME, recordKey).block();
                throw new BusinessRuntimeException(ResponseCode.STOCK_INSUFFICIENT);
            });

            log.info("[下单分析] orderId={} result=SUCCESS total={}ms | 锁={}ms | 查重={}ms | 写占位={}ms | Actor={}ms",
                    orderIdHolder[0], elapsedMs(requestStart), lockResult.getTryLockMs(), phaseCosts[0], phaseCosts[1], phaseCosts[2]);
            return lockResult.getResult();
        } catch (Exception e) {
            log.info("[下单分析] orderId={} result=FAIL total={}ms | 查重={}ms | 写占位={}ms | Actor={}ms | 原因={}",
                    orderIdHolder[0], elapsedMs(requestStart), phaseCosts[0], phaseCosts[1], phaseCosts[2], e.getMessage());
            throw e;
        }
    }

    /**
     * 查询票务剩余库存
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
     */
    @PostMapping("/payment/confirm")
    public Result<String> confirmPayment(@RequestBody OrderInfo orderInfoReq) {
        String orderId = orderInfoReq.getOrderId();
        long start = System.nanoTime();
        long queryOrderMs = 0L;
        long actorConfirmMs = 0L;

        if (StringUtils.isBlank(orderId)) {
            throw new BusinessRuntimeException(ResponseCode.BAD_REQUEST);
        }

        long queryStart = System.nanoTime();
        OrderInfo orderInfo = orderInfoService.lambdaQuery().eq(OrderInfo::getOrderId, orderId).one();
        queryOrderMs = elapsedMs(queryStart);

        if (orderInfo == null) {
            throw new BusinessRuntimeException(ResponseCode.ORDER_NOT_FOUND);
        }
        if (orderInfo.getStatus() == OrderConstants.ORDER_STATUS_PAID) {
            throw new BusinessRuntimeException(ResponseCode.ORDER_ALREADY_PAID);
        }
        if (orderInfo.getStatus() == OrderConstants.ORDER_STATUS_CANCELLED) {
            throw new BusinessRuntimeException(ResponseCode.ORDER_ALREADY_CANCELLED);
        }

        TicketActor ticketActor = new ActorProxyBuilder<>(TicketActor.class, DaprClientHolder.getActorClient())
                .build(new ActorId(orderInfo.getTicketId()));

        long confirmStart = System.nanoTime();
        ticketActor.confirmPayment(orderInfo).block();
        actorConfirmMs = elapsedMs(confirmStart);

        log.info("[支付分析] orderId={} total={}ms | 查订单={}ms | 支付确认={}ms",
                orderId, elapsedMs(start), queryOrderMs, actorConfirmMs);

        return Result.success("支付确认成功", orderId);
    }

    private long elapsedMs(long startNano) {
        return (System.nanoTime() - startNano) / 1_000_000;
    }
}
