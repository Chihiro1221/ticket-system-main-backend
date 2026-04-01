package com.haonan.ticketsystemmainbackend;

import com.haonan.ticketsystemmainbackend.actor.TicketActor;
import io.dapr.actors.ActorId;
import io.dapr.actors.client.ActorClient;
import io.dapr.actors.client.ActorProxyBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;

@SpringBootTest
class TicketSystemMainBackendApplicationTests {

    @Test
    public void runTest() {

        // 1. 初始化 Dapr 的 Actor 客户端 (用 try-with-resources 自动关闭)
        try (ActorClient client = new ActorClient()) {

            // 2. 指定你要抢购的商品/场次 ID
            // 极其关键：这其实就是你的“分布式锁的粒度”！不同的ID互不影响。
            ActorId actorId = new ActorId("Concert-JayChou-2026");

            // 3. 构建代理对象 (告诉 Dapr 我要调用哪个类型的 Actor)
            ActorProxyBuilder<TicketActor> builder =
                    new ActorProxyBuilder<>(TicketActor.class, client);

            // 生成代理实例，传入 ActorId 和 注解上的 name
            TicketActor actorProxy = builder.build(actorId);

            // 4. 发起调用：尝试扣减 1 张票！
            // 因为返回的是 Mono，我们用 .block() 阻塞等待结果拿到
            System.out.println("开始发起抢票请求...");
            //Boolean isSuccess = actorProxy.deductTicket(1).block();
            //System.out.println(isSuccess);
            // 在你的测试类 TestTicketClient 中
            System.out.println(">>> 准备调用 DeductTicket...");

            Mono<Boolean> mono = actorProxy.deductTicket(1);

// 打印这个 Mono 对象本身是否为空（理论上不为空）
            System.out.println(">>> Mono 对象: " + mono);

// 使用 subscribe 来详细观测底层发生了什么
            mono.subscribe(
                    result -> System.out.println(">>> 接收到结果: " + result),
                    error -> System.err.println(">>> 发生异常: " + error.getMessage()),
                    () -> System.out.println(">>> 流结束了 (Complete)")
            );

// 最后再用 block 拿结果
            Boolean isSuccess = mono.block();
            System.out.println(">>> 最终结果: " + isSuccess);

            if (isSuccess != null && isSuccess) {
                System.out.println("🎉 抢票成功！");
            } else {
                System.out.println("❌ 手慢了，库存不足！");
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

}
