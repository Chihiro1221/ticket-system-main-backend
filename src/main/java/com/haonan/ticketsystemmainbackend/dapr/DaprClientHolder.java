package com.haonan.ticketsystemmainbackend.dapr;

import io.dapr.actors.client.ActorClient;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;

public class DaprClientHolder {
    // 创建一个全局单例
    private static final DaprClient CLIENT = new DaprClientBuilder().build();

    private static final ActorClient ACTOR_CLIENT = new ActorClient();

    public static ActorClient getActorClient() {
        return ACTOR_CLIENT;
    }

    public static DaprClient getClient() {
        return CLIENT;
    }
}
