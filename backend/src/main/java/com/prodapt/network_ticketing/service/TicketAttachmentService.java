package com.prodapt.network_ticketing.service;

import com.prodapt.network_ticketing.entity.TicketAttachment;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface TicketAttachmentService {
    TicketAttachment saveAttachment(Long ticketId, MultipartFile file) throws IOException;
    List<TicketAttachment> listForTicket(Long ticketId);
    TicketAttachment getAttachment(Long id);
}
