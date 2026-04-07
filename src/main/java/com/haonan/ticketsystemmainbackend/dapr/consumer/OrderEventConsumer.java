package com.haonan.ticketsystemmainbackend.dapr.consumer;

import com.haonan.ticketsystemmainbackend.common.constants.DaprConstants;
import com.haonan.ticketsystemmainbackend.common.constants.OrderConstants;
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

    @Topic(pubsubName = DaprConstants.PUBSUB_NAME, name = DaprConstants.TOPIC_ORDER)
    @PostMapping("/handleOrder")
    @Transactional(rollbackFor = Exception.class)
    public void handleOrder(@RequestBody CloudEvent<OrderCreatedEvent> cloudEvent) {
        OrderCreatedEvent event = cloudEvent.getData();
        long totalStart = System.nanoTime();
        long stockUpdateStart = System.nanoTime();

        boolean updated = ticketStockService.lambdaUpdate()
                .eq(TicketStock::getId, event.getTicketId())
                .ge(TicketStock::getStock, event.getStock_count())
                .setDecrBy(TicketStock::getStock, event.getStock_count())
                .update();
        long stockUpdateMs = elapsedMs(stockUpdateStart);

        if (!updated) {
            log.error("库存扣减失败，可能是库存不足或并发冲突: {}", event.getTicketId());
            throw new RuntimeException("库存扣减失败");
        }

        try {
            long orderSaveStart = System.nanoTime();
            OrderInfo orderInfo = OrderInfo.builder()
                    .ticketId(event.getTicketId())
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .status(OrderConstants.ORDER_STATUS_PENDING)
                    .build();
            orderInfoService.save(orderInfo);
            long orderSaveMs = elapsedMs(orderSaveStart);

            log.info("[订单消费分析] orderId={} total={}ms | 扣数据库库存={}ms | 保存订单={}ms",
                    event.getOrderId(), elapsedMs(totalStart), stockUpdateMs, orderSaveMs);
        } catch (DuplicateKeyException e) {
            log.warn("订单重复处理，自动忽略: {}", event.getOrderId());
            throw e;
        }
    }

    private long elapsedMs(long startNano) {
        return (System.nanoTime() - startNano) / 1_000_000;
    }
}
