package com.prodapt.network_ticketing.service;

import com.prodapt.network_ticketing.dto.CreateTicketRequest;
import com.prodapt.network_ticketing.entity.Ticket;
import com.prodapt.network_ticketing.entity.TicketStatusHistory;

import java.util.List;

public interface TicketService {
    Ticket createTicket(CreateTicketRequest request);
    Ticket assignTicket(Long ticketId, Long engineerId, Long managerId, String priority);
    Ticket pickTicket(Long ticketId, Long engineerId);
    Ticket resolveTicket(Long ticketId, Long engineerId, String resolutionSummary);
    Ticket closeTicket(Long ticketId, Long customerId);
    Ticket reopenTicket(Long ticketId, Long customerId, String reopenReason);
    Ticket autoAssignTicket(Long ticketId);
    List<Ticket> getEngineerQueue(Long engineerId);
    List<Ticket> getTicketsForCustomer(Long customerId);
    List<Ticket> getTicketsForEngineer(Long engineerId);
    List<Ticket> getAllTickets();
    Ticket updatePriority(Long ticketId, String priority);
    Ticket assignEngineer(Long ticketId, Long engineerId);
    Ticket setSlaMinutes(Long ticketId, long minutes);
    Ticket addAiResolution(Long ticketId, String aiResolution);
    List<TicketStatusHistory> getTicketHistory(Long ticketId);
}
