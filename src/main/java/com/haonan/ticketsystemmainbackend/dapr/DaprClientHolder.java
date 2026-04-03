package com.haonan.ticketsystemmainbackend.dapr;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;

public class DaprClientHolder {
    // 创建一个全局单例
    private static final DaprClient CLIENT = new DaprClientBuilder().build();

    public static DaprClient getClient() {
        return CLIENT;
    }
}
