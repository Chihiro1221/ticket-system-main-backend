package com.haonan.ticketsystemmainbackend.service;

import com.haonan.ticketsystemmainbackend.domain.OrderInfo;
import com.haonan.ticketsystemmainbackend.dto.OrderSummaryResponse;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
 * @author heart
 * @description 针对表【order_info(订单表)】的数据库操作Service
 * @createDate 2026-04-02 12:13:37
 */
public interface OrderInfoService extends IService<OrderInfo> {
    boolean cancelOrderAndRestoreStock(String orderId);

    List<OrderSummaryResponse> listCurrentUserOrders(String userId);
}
