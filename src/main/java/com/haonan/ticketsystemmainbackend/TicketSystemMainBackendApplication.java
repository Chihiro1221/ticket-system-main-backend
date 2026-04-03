package com.haonan.ticketsystemmainbackend;

import com.haonan.ticketsystemmainbackend.actor.TicketActorImpl;
import com.haonan.ticketsystemmainbackend.service.OrderInfoService;
import com.haonan.ticketsystemmainbackend.service.TicketStockService;
import io.dapr.actors.runtime.ActorRuntime;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.haonan")
@MapperScan("com.haonan.ticketsystemmainbackend.mapper")
public class TicketSystemMainBackendApplication {

    @Resource
    private OrderInfoService orderInfoService;
    @Resource
    private TicketStockService ticketStockService;

    public static void main(String[] args) {
        SpringApplication.run(TicketSystemMainBackendApplication.class, args);
    }

    // 2. 在启动时，将 Service 传给 Actor 的工厂
    @PostConstruct
    public void registerActors() {
        ActorRuntime.getInstance().registerActor(
                TicketActorImpl.class,
                (context, id) -> new TicketActorImpl(context, id, ticketStockService, orderInfoService)
        );
    }

}
