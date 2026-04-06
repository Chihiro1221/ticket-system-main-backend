package com.haonan.ticketsystemmainbackend.actor;

import com.haonan.ticketsystemmainbackend.common.constants.DaprConstants;
import com.haonan.ticketsystemmainbackend.common.constants.OrderConstants;
import com.haonan.ticketsystemmainbackend.common.constants.SystemConstants;
import com.haonan.ticketsystemmainbackend.dapr.DaprClientHolder;
import com.haonan.ticketsystemmainbackend.domain.OrderCreatedEvent;
import com.haonan.ticketsystemmainbackend.domain.OrderInfo;
import com.haonan.ticketsystemmainbackend.domain.TicketStock;
import com.haonan.ticketsystemmainbackend.domain.TicketOrderCommand;
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

// 注解指定 Actor 的类型名称，网关调用时要用到
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
    public Mono<Boolean> deductTicket(TicketOrderCommand command) {
        return loadCurrentStock()
                .flatMap(currentStock -> {
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

                        log.info(">>> 扣减库存，当前库存: {}, 扣减数量: {}, 新库存: {}, 订单ID: {}", currentStock, count, newStock, orderId);
                        DaprClient client = DaprClientHolder.getClient();
                        return registerReminder(
                                SystemConstants.REMINDER_PREFIX_ORDER_TIMEOUT + orderId,
                                reminderOrder,
                                Duration.ofMinutes(SystemConstants.ORDER_TIMEOUT_MINUTES),
                                Duration.ofMillis(SystemConstants.REMINDER_PERIOD_ONCE)
                        ).then(getActorStateManager().set(SystemConstants.ACTOR_STOCK_KEY, newStock))
                                .then(getActorStateManager().save())
                                .then(client.publishEvent(DaprConstants.PUBSUB_NAME, DaprConstants.TOPIC_ORDER, event))
                                .thenReturn(true)
                                .onErrorResume(e -> compensateOrderCreationFailure(orderId, currentStock, e));
                    }
                    return Mono.just(false);
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
        return Mono.fromCallable(() -> orderInfoService.lambdaUpdate()
                        .eq(OrderInfo::getOrderId, orderId)
                        .eq(OrderInfo::getStatus, OrderConstants.ORDER_STATUS_PENDING)
                        .set(OrderInfo::getStatus, OrderConstants.ORDER_STATUS_PAID)
                        .update())
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(success -> {
                    if (success) {
                        log.info("订单 {} 数据库更新成功，正在撤销回收闹钟...", orderId);
                        return ticketStockService.finalizePurchaseRecord(userId, ticketId)
                                .then(unregisterReminder(SystemConstants.REMINDER_PREFIX_ORDER_TIMEOUT + orderId));
                        // 只有更新成功（确认支付），才撕掉闹钟
                    } else {
                        log.warn("订单 {} 状态变迁失败，闹钟可能已触发或订单异常", orderId);
                        // Todo: 触发退款逻辑.....
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
        // 根据闹钟名字判断要做什么（因为一个 Actor 可能有多个闹钟）
        if (reminderName.startsWith(SystemConstants.REMINDER_PREFIX_ORDER_TIMEOUT)) {
            // 处理订单超时
            return handleOrderTimeout(orderInfo);
        }
        return Mono.empty();
    }

    // 具体的超时处理逻辑
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

    /**
     * 订单创建失败
     * @param orderId
     * @param currentStock
     * @param throwable
     * @return
     */
    private Mono<Boolean> compensateOrderCreationFailure(String orderId, int currentStock, Throwable throwable) {
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
}
