package com.haonan.ticketsystemmainbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haonan.ticketsystemmainbackend.common.constants.DaprConstants;
import com.haonan.ticketsystemmainbackend.common.constants.OrderConstants;
import com.haonan.ticketsystemmainbackend.dapr.DaprClientHolder;
import com.haonan.ticketsystemmainbackend.domain.TicketStock;
import com.haonan.ticketsystemmainbackend.mapper.TicketStockMapper;
import com.haonan.ticketsystemmainbackend.service.TicketStockService;
import io.dapr.client.domain.SaveStateRequest;
import io.dapr.client.domain.State;
import io.dapr.client.domain.StateOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * @author heart
 * @description 针对表【ticket_stock(库存表)】的数据库操作Service实现
 * @createDate 2026-04-02 09:47:37
 */
@Service
@Slf4j
public class TicketStockServiceImpl extends ServiceImpl<TicketStockMapper, TicketStock>
        implements TicketStockService {

    // 修改购买记录缓存
    public Mono<Void> finalizePurchaseRecord(String userId, String ticketId) {
        String recordKey = String.format(DaprConstants.KEY_PURCHASE_RECORD_FORMAT, userId, ticketId);
        return DaprClientHolder.getClient().deleteState(DaprConstants.STATE_STORE_NAME, recordKey)
                .then(DaprClientHolder.getClient().saveState(DaprConstants.STATE_STORE_NAME, recordKey, OrderConstants.ORDER_STATUS_STR_PAID))
                .doOnSuccess(v -> log.info("成功转正订单: {}", recordKey)).doOnError(e -> log.error("转账失败订单: {}", e.getMessage()));
    }
}




