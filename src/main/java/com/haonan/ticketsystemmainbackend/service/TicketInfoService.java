package com.haonan.ticketsystemmainbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.haonan.ticketsystemmainbackend.domain.TicketInfo;
import com.haonan.ticketsystemmainbackend.dto.TicketCreateRequest;
import com.haonan.ticketsystemmainbackend.dto.TicketUpdateRequest;
import java.util.List;

/**
 * 票据基础信息 Service
 */
public interface TicketInfoService extends IService<TicketInfo> {

    TicketInfo createTicket(TicketCreateRequest request);

    TicketInfo updateTicket(String ticketId, TicketUpdateRequest request);

    void deleteTicket(String ticketId);

    TicketInfo getTicketDetail(String ticketId);

    List<TicketInfo> listTickets(String name, Integer status);
}
