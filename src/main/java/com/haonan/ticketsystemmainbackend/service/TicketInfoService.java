package com.haonan.ticketsystemmainbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.haonan.ticketsystemmainbackend.domain.TicketInfo;
import com.haonan.ticketsystemmainbackend.dto.TicketCreateRequest;
import com.haonan.ticketsystemmainbackend.dto.TicketInventoryResponse;
import com.haonan.ticketsystemmainbackend.dto.TicketUpdateRequest;
import java.util.List;

/**
 * 票据基础信息 Service
 */
public interface TicketInfoService extends IService<TicketInfo> {

    TicketInfo createTicket(TicketCreateRequest request);

    TicketInfo updateTicket(String ticketId, TicketUpdateRequest request);

    void deleteTicket(String ticketId);

    TicketInventoryResponse getTicketDetail(String ticketId);

    List<TicketInventoryResponse> listTickets(String name, Integer status);
}
