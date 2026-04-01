package com.haonan.ticketsystemmainbackend;

import com.haonan.ticketsystemmainbackend.actor.TicketActorImpl;
import io.dapr.actors.runtime.ActorRuntime;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.haonan")
public class TicketSystemMainBackendApplication {

    public static void main(String[] args) {
        // 1. 在 Spring 容器启动前，向 Dapr 运行时注册我们的 Actor
        // 第二个参数就是你说的 createActor 的 Lambda 实现：告诉 Dapr 如何 new 这个对象
        ActorRuntime.getInstance().registerActor(
                TicketActorImpl.class,
                TicketActorImpl::new
        );
        SpringApplication.run(TicketSystemMainBackendApplication.class, args);
    }

}
