package com.haonan.ticketsystemmainbackend.service;

import com.haonan.ticketsystemmainbackend.domain.TicketStock;
import com.baomidou.mybatisplus.extension.service.IService;
import reactor.core.publisher.Mono;

/**
 * @author heart
 * @description 针对表【ticket_stock(库存表)】的数据库操作Service
 * @createDate 2026-04-02 09:47:37
 */
public interface TicketStockService extends IService<TicketStock> {
    Mono<Void> finalizePurchaseRecord(String userId, String ticketId);
}
