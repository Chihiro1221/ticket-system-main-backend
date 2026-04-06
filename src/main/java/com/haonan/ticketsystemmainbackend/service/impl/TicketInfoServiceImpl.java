package com.haonan.ticketsystemmainbackend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haonan.ticketsystemmainbackend.common.ResponseCode;
import com.haonan.ticketsystemmainbackend.domain.TicketInfo;
import com.haonan.ticketsystemmainbackend.domain.TicketStock;
import com.haonan.ticketsystemmainbackend.dto.TicketCreateRequest;
import com.haonan.ticketsystemmainbackend.dto.TicketUpdateRequest;
import com.haonan.ticketsystemmainbackend.exception.BusinessRuntimeException;
import com.haonan.ticketsystemmainbackend.mapper.TicketInfoMapper;
import com.haonan.ticketsystemmainbackend.service.TicketInfoService;
import com.haonan.ticketsystemmainbackend.service.TicketStockService;
import jakarta.annotation.Resource;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 票据基础信息 Service 实现
 */
@Service
@Slf4j
public class TicketInfoServiceImpl extends ServiceImpl<TicketInfoMapper, TicketInfo>
        implements TicketInfoService {

    @Resource
    private TicketStockService ticketStockService;

    @Override
    @Transactional
    public TicketInfo createTicket(TicketCreateRequest request) {
        if (this.getById(request.getTicketId()) != null) {
            throw new BusinessRuntimeException(ResponseCode.BAD_REQUEST, "票据ID已存在，不能重复创建");
        }

        TicketInfo ticketInfo = new TicketInfo();
        BeanUtils.copyProperties(request, ticketInfo);
        boolean saved = this.save(ticketInfo);
        if (!saved) {
            throw new BusinessRuntimeException(ResponseCode.DATABASE_ERROR, "新增票据失败");
        }

        log.info("创建票据基础信息成功，ticketId={}", request.getTicketId());
        return ticketInfo;
    }

    @Override
    @Transactional
    public TicketInfo updateTicket(String ticketId, TicketUpdateRequest request) {
        TicketInfo existingTicket = this.getById(ticketId);
        if (existingTicket == null) {
            throw new BusinessRuntimeException(ResponseCode.TICKET_NOT_FOUND);
        }

        TicketInfo ticketInfo = new TicketInfo();
        BeanUtils.copyProperties(request, ticketInfo);
        ticketInfo.setTicketId(ticketId);

        boolean updated = this.updateById(ticketInfo);
        if (!updated) {
            throw new BusinessRuntimeException(ResponseCode.DATABASE_ERROR, "更新票据失败");
        }

        TicketStock stock = ticketStockService.getById(ticketId);
        if (stock != null && !StringUtils.equals(stock.getName(), request.getName())) {
            stock.setName(request.getName());
            boolean stockUpdated = ticketStockService.updateById(stock);
            if (!stockUpdated) {
                throw new BusinessRuntimeException(ResponseCode.DATABASE_ERROR, "票据名称同步库存表失败");
            }
        }

        log.info("更新票据基础信息成功，ticketId={}", ticketId);
        return this.getById(ticketId);
    }

    @Override
    @Transactional
    public void deleteTicket(String ticketId) {
        TicketInfo existingTicket = this.getById(ticketId);
        if (existingTicket == null) {
            throw new BusinessRuntimeException(ResponseCode.TICKET_NOT_FOUND);
        }

        if (ticketStockService.getById(ticketId) != null) {
            throw new BusinessRuntimeException(ResponseCode.BAD_REQUEST, "该票据已存在库存记录，请先处理库存数据后再删除");
        }

        boolean removed = this.removeById(ticketId);
        if (!removed) {
            throw new BusinessRuntimeException(ResponseCode.DATABASE_ERROR, "删除票据失败");
        }

        log.info("删除票据基础信息成功，ticketId={}", ticketId);
    }

    @Override
    public TicketInfo getTicketDetail(String ticketId) {
        TicketInfo ticketInfo = this.getById(ticketId);
        if (ticketInfo == null) {
            throw new BusinessRuntimeException(ResponseCode.TICKET_NOT_FOUND);
        }
        return ticketInfo;
    }

    @Override
    public List<TicketInfo> listTickets(String name, Integer status) {
        return this.list(Wrappers.<TicketInfo>lambdaQuery()
                .like(StringUtils.isNotBlank(name), TicketInfo::getName, name)
                .eq(status != null, TicketInfo::getStatus, status)
                .orderByDesc(TicketInfo::getCreateTime));
    }
}
