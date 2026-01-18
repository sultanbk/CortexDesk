package com.prodapt.network_ticketing.repository;

import com.prodapt.network_ticketing.entity.TicketStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketStatusHistoryRepository
        extends JpaRepository<TicketStatusHistory, Long> {

    List<TicketStatusHistory> findByTicket_TicketId(Long ticketId);
}
