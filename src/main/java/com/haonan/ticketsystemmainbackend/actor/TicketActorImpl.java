package com.haonan.ticketsystemmainbackend.actor;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.haonan.ticketsystemmainbackend.common.constants.DaprConstants;
import com.haonan.ticketsystemmainbackend.common.constants.OrderConstants;
import com.haonan.ticketsystemmainbackend.common.constants.SystemConstants;
import com.haonan.ticketsystemmainbackend.dapr.DaprClientHolder;
import com.haonan.ticketsystemmainbackend.domain.OrderCreatedEvent;
import com.haonan.ticketsystemmainbackend.domain.OrderInfo;
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
    public Mono<Boolean> deductTicket(int count) {
        // 1. 直接尝试获取，利用 onErrorResume 处理“Key 不存在”的情况
        return super.getActorStateManager().get(SystemConstants.ACTOR_STOCK_KEY, Integer.class)
                .onErrorResume(e -> {
                    // 如果报错或找不到，说明是第一次加载，去查数据库
                    log.info(">>> Redis 未命中，尝试从数据库加载 ID: {}", this.getId());
                    return Mono.fromCallable(() -> {
                        // 使用动态 ID 查询数据库
                        TicketStock ts = ticketStockService.getById(this.getId().toString());
                        return ts != null ? ts.getStock() : 0;
                    }).subscribeOn(Schedulers.boundedElastic());
                })
                .flatMap(currentStock -> {
                    if (currentStock >= count) {
                        int newStock = currentStock - count;
                        log.info(">>> 扣减库存，当前库存: {}, 扣减数量: {}, 新库存: {}", currentStock, count, newStock);
                        // 直接通过静态方法调用
                        DaprClient client = DaprClientHolder.getClient();
                        String orderId = IdWorker.getIdStr();
                        // 注册 Reminder: 10分钟后触发，如果不取消，就回滚库存
                        // 参数：提醒名称, 关联数据, 延迟时间, 周期（这里设为-1表示只执行一次）
                        log.info("注册提醒器");
                        return registerReminder(
                                SystemConstants.REMINDER_PREFIX_ORDER_TIMEOUT + orderId,
                                OrderInfo.builder().orderId(orderId).ticketId(this.getId().toString()).build(),
                                Duration.ofMinutes(SystemConstants.ORDER_TIMEOUT_MINUTES),
                                Duration.ofMillis(SystemConstants.REMINDER_PERIOD_ONCE)
                        ).then(
                                getActorStateManager().set(SystemConstants.ACTOR_STOCK_KEY, newStock)
                        ).then(Mono.defer(() -> {
                            // 发送消息队列扣减库存
                            return client.publishEvent(DaprConstants.PUBSUB_NAME, DaprConstants.TOPIC_ORDER, OrderCreatedEvent.builder()
                                    .ticketId(this.getId().toString())
                                    .orderId(orderId)
                                    .timestamp(System.currentTimeMillis())
                                    .build()
                            );
                        })).thenReturn(true);
                    }
                    return Mono.just(false);
                });
    }

    @Override
    public Mono<Integer> getRestCount() {
        return super.getActorStateManager().get(SystemConstants.ACTOR_STOCK_KEY, Integer.class)
                .defaultIfEmpty(SystemConstants.DEFAULT_STOCK);
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
                        ticketStockService.finalizePurchaseRecord(userId, ticketId).block();
                        // 只有更新成功（确认支付），才撕掉闹钟
                        return unregisterReminder(SystemConstants.REMINDER_PREFIX_ORDER_TIMEOUT + orderId);
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
        // 1. 将阻塞的数据库操作包装起来
        return Mono.fromCallable(() -> orderInfoService.cancelOrderAndRestoreStock(orderId))
                .subscribeOn(Schedulers.boundedElastic()) // 【核心】切换到专门处理 I/O 的线程池
                .flatMap(dbSuccess -> {
                    if (!dbSuccess) {
                        log.info("订单 {} 无需回收（可能已支付或已手动取消）", orderId);
                        return Mono.empty();
                    }
                    String recordKey = String.format(DaprConstants.KEY_PURCHASE_RECORD_LEGACY_FORMAT, userId, ticketId);
                    DaprClientHolder.getClient().deleteState(DaprConstants.STATE_STORE_NAME, recordKey)
                            .doOnSuccess(v -> log.info("用户 {} 的限购记录已清除，可重新购票", userId));
                    // 只有数据库操作成功了（确认回收了），才去更新 Actor 的内存状态
                    return getActorStateManager().get(SystemConstants.ACTOR_STOCK_KEY, Integer.class)
                            .flatMap(currentStock -> {
                                int restoredStock = currentStock + SystemConstants.STOCK_RESTORE_COUNT;
                                log.info("Actor 内存库存回滚：{} -> {}", currentStock, restoredStock);
                                return getActorStateManager().set(SystemConstants.ACTOR_STOCK_KEY, restoredStock)
                                        .then(getActorStateManager().save());
                            });
                })
                .doOnSuccess(v -> log.info("订单 {} 全链路回收任务完成", orderId))
                .doOnError(e -> log.error("回收订单 {} 失败: {}", orderId, e.getMessage()))
                .then();
    }
}