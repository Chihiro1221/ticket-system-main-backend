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
        return executeWithLockMetrics(userId, action).getResult();
    }

    public <T> LockExecutionResult<T> executeWithLockMetrics(String userId, LockAction<T> action) {
        String lockOwner = UUID.randomUUID().toString();
        String resourceId = DaprConstants.USER_LOCK_PREFIX + userId;

        boolean acquired = false;
        long totalStart = System.nanoTime();
        long tryLockStart = totalStart;
        long tryLockMs = 0L;
        long businessMs = 0L;
        long unlockMs = 0L;
        T result = null;

        try {
            acquired = tryLock(resourceId, lockOwner);
            tryLockMs = elapsedMs(tryLockStart);
            if (!acquired) {
                throw new BusinessRuntimeException(ResponseCode.TOO_MANY_REQUESTS);
            }

            long businessStart = System.nanoTime();
            result = action.execute();
            businessMs = elapsedMs(businessStart);
        } finally {
            if (acquired) {
                long unlockStart = System.nanoTime();
                unlock(resourceId, lockOwner);
                unlockMs = elapsedMs(unlockStart);
            }
        }

        return new LockExecutionResult<>(result, tryLockMs, businessMs, unlockMs, elapsedMs(totalStart), acquired);
    }

    /**
     * 原子加锁
     */
    private boolean tryLock(String resourceId, String lockOwner) {
        try {
            LockRequest lockRequest = new LockRequest(
                    DaprConstants.LOCK_STORE_NAME,
                    resourceId,
                    lockOwner,
                    SystemConstants.LOCK_EXPIRY_SECONDS
            );

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
            UnlockRequest unlockRequest = new UnlockRequest(
                    DaprConstants.LOCK_STORE_NAME,
                    resourceId,
                    lockOwner
            );

            UnlockResponseStatus status = previewClient.unlock(unlockRequest).block();
            if (status != UnlockResponseStatus.SUCCESS) {
                log.warn("资源 {} 释放失败或已过期，当前状态: {}", resourceId, status.name());
            }
        } catch (Exception e) {
            log.error("Dapr 释放锁异常 - 资源: {}, 错误: {}", resourceId, e.getMessage());
        }
    }

    private long elapsedMs(long startNano) {
        return (System.nanoTime() - startNano) / 1_000_000;
    }

    @FunctionalInterface
    public interface LockAction<T> {
        T execute();
    }

    public static class LockExecutionResult<T> {
        private final T result;
        private final long tryLockMs;
        private final long businessMs;
        private final long unlockMs;
        private final long totalMs;
        private final boolean acquired;

        public LockExecutionResult(T result, long tryLockMs, long businessMs, long unlockMs, long totalMs, boolean acquired) {
            this.result = result;
            this.tryLockMs = tryLockMs;
            this.businessMs = businessMs;
            this.unlockMs = unlockMs;
            this.totalMs = totalMs;
            this.acquired = acquired;
        }

        public T getResult() {
            return result;
        }

        public long getTryLockMs() {
            return tryLockMs;
        }

        public long getBusinessMs() {
            return businessMs;
        }

        public long getUnlockMs() {
            return unlockMs;
        }

        public long getTotalMs() {
            return totalMs;
        }

        public boolean isAcquired() {
            return acquired;
        }
    }
}
