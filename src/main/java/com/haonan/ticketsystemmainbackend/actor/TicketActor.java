package com.haonan.ticketsystemmainbackend.actor;

import com.haonan.ticketsystemmainbackend.domain.OrderInfo;
import com.haonan.ticketsystemmainbackend.domain.TicketOrderCommand;
import io.dapr.actors.ActorMethod;
import io.dapr.actors.ActorType;
import io.dapr.actors.runtime.ActorFactory;
import reactor.core.publisher.Mono;

@ActorType(name = "TicketActor")
public interface TicketActor extends ActorFactory {
    
    // 扣减库存方法：传入要买的数量，返回是否成功 (true/false)
    @ActorMethod(returns = Boolean.class)
    Mono<Boolean> deductTicket(TicketOrderCommand command);

    // 支付成功回调（用于取消定时器）
    @ActorMethod(returns = Void.class)
    Mono<Void> confirmPayment(OrderInfo orderInfo);

    // 查询当前剩余库存
    Mono<Integer> getRestCount();
}
