package com.haonan.ticketsystemmainbackend.actor;

import com.haonan.ticketsystemmainbackend.common.constants.DaprConstants;
import com.haonan.ticketsystemmainbackend.common.constants.OrderConstants;
import com.haonan.ticketsystemmainbackend.common.constants.SystemConstants;
import com.haonan.ticketsystemmainbackend.dapr.DaprClientHolder;
import com.haonan.ticketsystemmainbackend.domain.OrderCreatedEvent;
import com.haonan.ticketsystemmainbackend.domain.OrderInfo;
import com.haonan.ticketsystemmainbackend.domain.TicketOrderCommand;
import com.haonan.ticketsystemmainbackend.domain.TicketStock;
import com.haonan.ticketsystemmainbackend.service.OrderInfoService;
import com.haonan.ticketsystemmainbackend.service.TicketStockService;
import io.dapr.actors.ActorId;
import io.dapr.actors.ActorType;
import io.dapr.actors.runtime.AbstractActor;
import io.dapr.actors.runtime.ActorRuntimeContext;
import io.dapr.actors.runtime.Remindable;
import io.dapr.client.DaprClient;
import io.dapr.utils.TypeRef;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

@ActorType(name = "TicketActor")
@Slf4j
public class TicketActorImpl extends AbstractActor implements TicketActor, Remindable {
    private final TicketStockService ticketStockService;
    private final OrderInfoService orderInfoService;

    public TicketActorImpl(ActorRuntimeContext runtimeContext, ActorId id, TicketStockService ticketStockService, OrderInfoService orderInfoService) {
        super(runtimeContext, id);
        this.ticketStockService = ticketStockService;
        this.orderInfoService = orderInfoService;
    }

    @Override
    public Mono<Integer> deductTicket(TicketOrderCommand command) {
        long totalStart = System.nanoTime();
        long queueWaitMs = command.getActorInvokeAtMs() == null ? -1L : Math.max(System.currentTimeMillis() - command.getActorInvokeAtMs(), 0L);
        return loadCurrentStock()
                .flatMap(currentStock -> {
                    long loadStockMs = elapsedMs(totalStart);
                    int count = command.getCount() == null ? SystemConstants.STOCK_DEDUCT_COUNT : command.getCount();
                    if (currentStock >= count) {
                        int newStock = currentStock - count;
                        String orderId = command.getOrderId();
                        String ticketId = command.getTicketId();
                        String userId = command.getUserId();
                        OrderInfo reminderOrder = OrderInfo.builder()
                                .orderId(orderId)
                                .ticketId(ticketId)
                                .userId(userId)
                                .status(OrderConstants.ORDER_STATUS_PENDING)
                                .build();
                        OrderCreatedEvent event = OrderCreatedEvent.builder()
                                .ticketId(ticketId)
                                .userId(userId)
                                .orderId(orderId)
                                .stock_count(count)
                                .timestamp(System.currentTimeMillis())
                                .build();

                        DaprClient client = DaprClientHolder.getClient();
                        long writeActorStart = System.nanoTime();
                        return registerReminder(
                                SystemConstants.REMINDER_PREFIX_ORDER_TIMEOUT + orderId,
                                reminderOrder,
                                Duration.ofMinutes(SystemConstants.ORDER_TIMEOUT_MINUTES),
                                Duration.ofMillis(SystemConstants.REMINDER_PERIOD_ONCE)
                        ).then(getActorStateManager().set(SystemConstants.ACTOR_STOCK_KEY, newStock))
                                .then(getActorStateManager().save())
                                .then(client.publishEvent(DaprConstants.PUBSUB_NAME, DaprConstants.TOPIC_ORDER, event))
                                .thenReturn(newStock)
                                .doOnSuccess(restStock -> log.info(
                                        "[Actor分析] orderId={} 排队={}ms | 执行={}ms | 读库存={}ms | 写状态并发事件={}ms | 库存 {} -> {}",
                                        orderId, queueWaitMs, elapsedMs(totalStart), loadStockMs, elapsedMs(writeActorStart), currentStock, newStock
                                ))
                                .onErrorResume(e -> compensateOrderCreationFailure(orderId, currentStock, e));
                    }
                    log.info("[Actor分析] orderId={} result=STOCK_INSUFFICIENT 排队={}ms | 执行={}ms | 读库存={}ms | 库存={}",
                            command.getOrderId(), queueWaitMs, elapsedMs(totalStart), loadStockMs, currentStock);
                    return Mono.empty();
                });
    }

    @Override
    public Mono<Integer> getRestCount() {
        return loadCurrentStock();
    }

    @Override
    public Mono<Void> confirmPayment(OrderInfo orderInfo) {
        String orderId = orderInfo.getOrderId();
        String ticketId = orderInfo.getTicketId();
        String userId = orderInfo.getUserId();
        long totalStart = System.nanoTime();
        long updateOrderStart = System.nanoTime();

        return Mono.fromCallable(() -> orderInfoService.lambdaUpdate()
                        .eq(OrderInfo::getOrderId, orderId)
                        .eq(OrderInfo::getStatus, OrderConstants.ORDER_STATUS_PENDING)
                        .set(OrderInfo::getStatus, OrderConstants.ORDER_STATUS_PAID)
                        .update())
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(success -> {
                    long updateOrderMs = elapsedMs(updateOrderStart);
                    if (success) {
                        long finalizeStart = System.nanoTime();
                        return ticketStockService.finalizePurchaseRecord(userId, ticketId)
                                .then(unregisterReminder(SystemConstants.REMINDER_PREFIX_ORDER_TIMEOUT + orderId))
                                .doOnSuccess(v -> log.info(
                                        "[Actor支付分析] orderId={} total={}ms | 改订单状态={}ms | 清记录和闹钟={}ms",
                                        orderId, elapsedMs(totalStart), updateOrderMs, elapsedMs(finalizeStart)
                                ));
                    } else {
                        log.warn("订单 {} 状态变迁失败，闹钟可能已触发或订单异常", orderId);
                        return Mono.empty();
                    }
                })
                .doOnError(e -> log.error("支付确认逻辑异常: {}", e.getMessage()))
                .then();
    }

    @Override
    public AbstractActor createActor(ActorRuntimeContext actorRuntimeContext, ActorId actorId) {
        return new TicketActorImpl(actorRuntimeContext, actorId, ticketStockService, orderInfoService);
    }

    @Override
    public TypeRef<OrderInfo> getStateType() {
        return TypeRef.get(OrderInfo.class);
    }

    @Override
    public Mono<Void> receiveReminder(String reminderName, Object state, Duration dueTime, Duration period) {
        OrderInfo orderInfo = (OrderInfo) state;
        log.info("【Reminder】收到提醒: {}, 关联订单: {}", reminderName, orderInfo);
        if (reminderName.startsWith(SystemConstants.REMINDER_PREFIX_ORDER_TIMEOUT)) {
            return handleOrderTimeout(orderInfo);
        }
        return Mono.empty();
    }

    private Mono<Void> handleOrderTimeout(OrderInfo orderInfo) {
        String orderId = orderInfo.getOrderId();
        String userId = orderInfo.getUserId();
        String ticketId = orderInfo.getTicketId();
        log.warn("检测到订单 {} 超时提醒触发...", orderId);
        return Mono.fromCallable(() -> orderInfoService.getById(orderId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(existingOrder -> {
                    if (existingOrder == null) {
                        log.warn("订单 {} 未落库，执行 Actor 库存恢复和购买记录清理", orderId);
                        return clearPurchaseRecord(userId, ticketId)
                                .then(restoreActorStock(SystemConstants.STOCK_RESTORE_COUNT));
                    }
                    if (existingOrder.getStatus() == OrderConstants.ORDER_STATUS_PENDING) {
                        return Mono.fromCallable(() -> orderInfoService.cancelOrderAndRestoreStock(orderId))
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMap(dbSuccess -> {
                                    if (!dbSuccess) {
                                        log.info("订单 {} 无需回收（可能已支付或已手动取消）", orderId);
                                        return Mono.empty();
                                    }
                                    return clearPurchaseRecord(userId, ticketId)
                                            .then(restoreActorStock(SystemConstants.STOCK_RESTORE_COUNT));
                                });
                    }
                    if (existingOrder.getStatus() == OrderConstants.ORDER_STATUS_CANCELLED) {
                        return clearPurchaseRecord(userId, ticketId);
                    }
                    log.info("订单 {} 已支付，无需回收", orderId);
                    return Mono.empty();
                })
                .doOnSuccess(v -> log.info("订单 {} 全链路回收任务完成", orderId))
                .doOnError(e -> log.error("回收订单 {} 失败: {}", orderId, e.getMessage()))
                .then();
    }

    private Mono<Integer> loadCurrentStock() {
        return super.getActorStateManager().get(SystemConstants.ACTOR_STOCK_KEY, Integer.class)
                .onErrorResume(e -> {
                    log.warn("读取 Actor 库存状态失败，尝试从数据库加载，actorId={}", this.getId(), e);
                    return loadStockFromDatabase();
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.info(">>> Actor 状态未命中，尝试从数据库加载 ID: {}", this.getId());
                    return loadStockFromDatabase();
                }));
    }

    private Mono<Integer> compensateOrderCreationFailure(String orderId, int currentStock, Throwable throwable) {
        log.error("订单 {} 创建链路失败，开始补偿: {}", orderId, throwable.getMessage());
        return unregisterReminder(SystemConstants.REMINDER_PREFIX_ORDER_TIMEOUT + orderId)
                .onErrorResume(e -> Mono.empty())
                .then(getActorStateManager().set(SystemConstants.ACTOR_STOCK_KEY, currentStock))
                .then(getActorStateManager().save())
                .then(Mono.error(throwable));
    }

    private Mono<Void> clearPurchaseRecord(String userId, String ticketId) {
        if (userId == null || ticketId == null) {
            return Mono.empty();
        }
        String recordKey = String.format(DaprConstants.KEY_PURCHASE_RECORD_FORMAT, userId, ticketId);
        return DaprClientHolder.getClient().deleteState(DaprConstants.STATE_STORE_NAME, recordKey)
                .doOnSuccess(v -> log.info("用户 {} 的限购记录已清除，可重新购票", userId));
    }

    private Mono<Void> restoreActorStock(int restoreCount) {
        return super.getActorStateManager().get(SystemConstants.ACTOR_STOCK_KEY, Integer.class)
                .flatMap(currentStock -> {
                    int restoredStock = currentStock + restoreCount;
                    log.info("Actor 内存库存回滚：{} -> {}", currentStock, restoredStock);
                    return getActorStateManager().set(SystemConstants.ACTOR_STOCK_KEY, restoredStock)
                            .then(getActorStateManager().save());
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Actor 库存状态缺失，使用数据库库存重建 Actor 状态，actorId={}", this.getId());
                    return loadStockFromDatabase()
                            .flatMap(dbStock -> getActorStateManager().set(SystemConstants.ACTOR_STOCK_KEY, dbStock)
                                    .then(getActorStateManager().save()));
                }));
    }

    private Mono<Integer> loadStockFromDatabase() {
        return Mono.fromCallable(() -> {
            TicketStock ts = ticketStockService.getById(this.getId().toString());
            return ts != null ? ts.getStock() : 0;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private long elapsedMs(long startNano) {
        return (System.nanoTime() - startNano) / 1_000_000;
    }
}
