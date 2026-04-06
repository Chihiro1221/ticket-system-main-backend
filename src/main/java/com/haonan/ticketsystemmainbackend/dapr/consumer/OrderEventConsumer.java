package com.haonan.ticketsystemmainbackend.dapr.consumer;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.haonan.ticketsystemmainbackend.common.constants.DaprConstants;
import com.haonan.ticketsystemmainbackend.common.constants.SystemConstants;
import com.haonan.ticketsystemmainbackend.domain.OrderCreatedEvent;
import com.haonan.ticketsystemmainbackend.domain.OrderInfo;
import com.haonan.ticketsystemmainbackend.domain.TicketStock;
import com.haonan.ticketsystemmainbackend.service.OrderInfoService;
import com.haonan.ticketsystemmainbackend.service.TicketStockService;
import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Component
@RestController
@Slf4j
public class OrderEventConsumer {

    @Resource
    private TicketStockService ticketStockService;
    @Resource
    private OrderInfoService orderInfoService;

    // pubsubName 是你在 dapr/components/pubsub.yaml 中配置的 name
    // topic 是你发布消息时指定的 topic 名称
    @Topic(pubsubName = DaprConstants.PUBSUB_NAME, name = DaprConstants.TOPIC_ORDER)
    @PostMapping("/handleOrder")
    @Transactional(rollbackFor = Exception.class)
    public void handleOrder(@RequestBody CloudEvent<OrderCreatedEvent> cloudEvent) {
        log.info("收到订单消息: {}", cloudEvent.getData());
        OrderCreatedEvent event = cloudEvent.getData();
        // 1. 必须先扣库存 (原子操作)
        // 利用数据库的行锁特性，确保强一致性
        boolean updated = ticketStockService.lambdaUpdate()
                .eq(TicketStock::getId, event.getTicketId())
                .gt(TicketStock::getStock, 0) // 必须大于0才能扣
                .setDecrBy(TicketStock::getStock, SystemConstants.STOCK_DEDUCT_COUNT)
                .update();

        if (!updated) {
            log.error("库存扣减失败，可能是库存不足或并发冲突: {}", event.getTicketId());
            // 抛出异常触发事务回滚，防止订单插入
            throw new RuntimeException("库存扣减失败");
        }

        // 2. 插入订单 (利用唯一索引防重)
        try {
            OrderInfo orderInfo = OrderInfo.builder()
                    .ticketId(event.getTicketId())
                    .orderId(event.getOrderId()) // 建议这里的 orderId 就是 event 里的 ID
                    .userId(IdWorker.getIdStr())   // 应该从事件里取，不要生成假的
                    .build();
            orderInfoService.save(orderInfo);
        } catch (DuplicateKeyException e) {
            // 如果触发了唯一索引冲突，说明是重复消息，抛异常回滚，这样库存扣减也不会生效
            log.warn("订单重复处理，自动忽略: {}", event.getOrderId());
            throw e;
        }
    }
}