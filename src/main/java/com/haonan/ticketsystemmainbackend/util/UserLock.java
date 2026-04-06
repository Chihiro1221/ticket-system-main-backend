package com.haonan.ticketsystemmainbackend.util;

import com.haonan.ticketsystemmainbackend.common.ResponseCode;
import com.haonan.ticketsystemmainbackend.common.constants.DaprConstants;
import com.haonan.ticketsystemmainbackend.common.constants.SystemConstants;
import com.haonan.ticketsystemmainbackend.exception.BusinessRuntimeException;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprPreviewClient;
import io.dapr.client.domain.LockRequest;
import io.dapr.client.domain.UnlockRequest;
import io.dapr.client.domain.UnlockResponseStatus;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.UUID;

/**
 * 完美版用户分布式锁
 * 基于 Dapr Preview Client (Distributed Lock API) 实现，保证原子性
 */
@Component
@Slf4j
public class UserLock {

    // 分布式锁属于 Preview API，需要使用专门的 PreviewClient
    private final DaprPreviewClient previewClient;

    public UserLock() {
        // 初始化 Preview Client
        this.previewClient = new DaprClientBuilder().buildPreviewClient();
    }

    // 优雅关闭客户端
    @PreDestroy
    public void close() {
        try {
            if (previewClient != null) {
                previewClient.close();
            }
        } catch (Exception e) {
            log.error("关闭 DaprPreviewClient 失败", e);
        }
    }

    /**
     * 带锁执行操作（推荐用法）
     */
    public <T> T executeWithLock(String userId, LockAction<T> action) {
        // 1. 生成唯一的锁持有者 ID (OwnerId)
        String lockOwner = UUID.randomUUID().toString();
        String resourceId = DaprConstants.USER_LOCK_PREFIX + userId;

        boolean acquired = false;
        try {
            acquired = tryLock(resourceId, lockOwner);
            if (!acquired) {
                throw new BusinessRuntimeException(ResponseCode.TOO_MANY_REQUESTS);
            }
            return action.execute();
        } finally {
            if (acquired) {
                unlock(resourceId, lockOwner);
            }
        }
    }

    /**
     * 原子加锁
     */
    private boolean tryLock(String resourceId, String lockOwner) {
        try {
            // 构建加锁请求对象
            LockRequest lockRequest = new LockRequest(
                    DaprConstants.LOCK_STORE_NAME,
                    resourceId,
                    lockOwner,
                    SystemConstants.LOCK_EXPIRY_SECONDS
            );

            // 调用 tryLock 并阻塞获取结果 (Mono<Boolean>)
            Boolean success = previewClient.tryLock(lockRequest).block();
            return success != null && success;
        } catch (Exception e) {
            log.error("Dapr 加锁异常 - 资源: {}, 错误: {}", resourceId, e.getMessage());
            return false;
        }
    }

    /**
     * 原子释放锁
     */
    private void unlock(String resourceId, String lockOwner) {
        try {
            // 构建释放锁请求对象
            UnlockRequest unlockRequest = new UnlockRequest(
                    DaprConstants.LOCK_STORE_NAME,
                    resourceId,
                    lockOwner
            );

            // 调用 unlock 并获取状态 (Mono<UnlockResponseStatus>)
            UnlockResponseStatus status = previewClient.unlock(unlockRequest).block();

            // UnlockResponseStatus 是一个枚举，包含了 SUCCESS, LOCK_DOES_NOT_EXIST, LOCK_BELONGS_TO_OTHERS 等状态
            if (status == UnlockResponseStatus.SUCCESS) {
                log.info("资源 {} 释放成功", resourceId);
            } else {
                log.warn("资源 {} 释放失败或已过期，当前状态: {}", resourceId, status.name());
            }
        } catch (Exception e) {
            log.error("Dapr 释放锁异常 - 资源: {}, 错误: {}", resourceId, e.getMessage());
        }
    }

    @FunctionalInterface
    public interface LockAction<T> {
        T execute();
    }
}