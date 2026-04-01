package com.haonan.ticketsystemmainbackend.actor;

import io.dapr.actors.ActorType;
import io.dapr.actors.runtime.ActorFactory;
import reactor.core.publisher.Mono;

@ActorType(name = "TicketActor")
public interface TicketActor extends ActorFactory {
    
    // 扣减库存方法：传入要买的数量，返回是否成功 (true/false)
    Mono<Boolean> deductTicket(int count);

    // 查询当前剩余库存
    Mono<Integer> getRestCount();
}
