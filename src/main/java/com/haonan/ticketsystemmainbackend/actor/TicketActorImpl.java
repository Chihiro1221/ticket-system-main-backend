package com.haonan.ticketsystemmainbackend.actor;

import io.dapr.actors.ActorId;
import io.dapr.actors.ActorType;
import io.dapr.actors.runtime.AbstractActor;
import io.dapr.actors.runtime.ActorRuntimeContext;
import reactor.core.publisher.Mono;

// 注解指定 Actor 的类型名称，网关调用时要用到
@ActorType(name = "TicketActor")
public class TicketActorImpl extends AbstractActor implements TicketActor {

    // 状态存储的 Key
    private static final String STOCK_KEY = "stock_count";

    public TicketActorImpl(ActorRuntimeContext runtimeContext, ActorId id) {
        super(runtimeContext, id);
    }

    @Override
    public Mono<Boolean> deductTicket(int count) {
        // 1. 从 Dapr 的状态管理器中获取当前库存
        Mono<Boolean> booleanMono = super.getActorStateManager().get(STOCK_KEY, Integer.class)
                .onErrorReturn(100)
                .flatMap(currentStock -> {
                    // 2. 核心业务判断：如果库存足够
                    if (currentStock >= count) {
                        int newStock = currentStock - count;
                        System.out.println("ActorId: " + this.getId() + " 扣减成功! 剩余库存: " + newStock);

                        // 3. 更新状态并保存 (这一步会自动持久化到 Redis 里)
                        return super.getActorStateManager().set(STOCK_KEY, newStock)
                                .then(super.getActorStateManager().save())
                                .thenReturn(true);
                    } else {
                        // 库存不足，直接返回失败
                        System.out.println("ActorId: " + this.getId() + " 库存不足! 扣减失败");
                        return Mono.just(false);
                    }
                });
        return booleanMono;
    }

    @Override
    public Mono<Integer> getRestCount() {
        return super.getActorStateManager().get(STOCK_KEY, Integer.class)
                .defaultIfEmpty(100);
    }

    @Override
    public AbstractActor createActor(ActorRuntimeContext actorRuntimeContext, ActorId actorId) {
        return new TicketActorImpl(actorRuntimeContext, actorId);
    }
}