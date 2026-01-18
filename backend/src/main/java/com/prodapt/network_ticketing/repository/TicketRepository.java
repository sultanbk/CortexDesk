package com.prodapt.network_ticketing.repository;

import com.prodapt.network_ticketing.entity.Ticket;
import com.prodapt.network_ticketing.entity.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    // Customer view
    List<Ticket> findByCustomer_UserId(Long customerId);

    // Engineer view (assigned tickets)
    List<Ticket> findByAssignedEngineer_UserId(Long engineerId);

    // Engineer view (tickets available to pick)
    List<Ticket> findByStatus(String status);

    List<Ticket> findByStatusNot(TicketStatus status);

}
