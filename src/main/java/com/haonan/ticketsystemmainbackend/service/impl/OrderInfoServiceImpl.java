package com.haonan.ticketsystemmainbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haonan.ticketsystemmainbackend.common.constants.OrderConstants;
import com.haonan.ticketsystemmainbackend.common.constants.SystemConstants;
import com.haonan.ticketsystemmainbackend.domain.OrderInfo;
import com.haonan.ticketsystemmainbackend.domain.TicketStock;
import com.haonan.ticketsystemmainbackend.mapper.OrderInfoMapper;
import com.haonan.ticketsystemmainbackend.service.OrderInfoService;
import com.haonan.ticketsystemmainbackend.service.TicketStockService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author heart
 * @description 针对表【order_info(订单表)】的数据库操作Service实现
 * @createDate 2026-04-02 12:13:37
 */
@Service
@Slf4j
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo>
        implements OrderInfoService {

    @Resource
    private TicketStockService ticketStockService;

    @Transactional
    public boolean cancelOrderAndRestoreStock(String orderId) {
        boolean success = lambdaUpdate()
                .eq(OrderInfo::getOrderId, orderId)
                .eq(OrderInfo::getStatus, OrderConstants.ORDER_STATUS_PENDING) // 只有状态为 待支付 的时候才准许修改
                .set(OrderInfo::getStatus, OrderConstants.ORDER_STATUS_CANCELLED) // 修改为 已取消
                .update();
        if (!success) {
            log.info("订单 {} 状态已变迁，无需回收库存", orderId);
            return false;
        }
        // 先查一下对应的 ticketId (或者从入参传进来)
        OrderInfo orderInfo = this.getById(orderId);
        boolean stockRestored = ticketStockService.lambdaUpdate()
                .eq(TicketStock::getId, orderInfo.getTicketId())
                .setIncrBy(TicketStock::getStock, SystemConstants.STOCK_RESTORE_COUNT)
                .update();

        if (!stockRestored) {
            throw new RuntimeException("库存回滚失败，订单ID: " + orderId);
        }

        return true;
    }
}




