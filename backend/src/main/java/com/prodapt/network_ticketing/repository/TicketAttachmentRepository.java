package com.prodapt.network_ticketing.repository;

import com.prodapt.network_ticketing.entity.TicketAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Long> {
    List<TicketAttachment> findByTicketTicketId(Long ticketId);
}
