package com.prodapt.network_ticketing.service.impl;

import com.prodapt.network_ticketing.entity.Ticket;
import com.prodapt.network_ticketing.service.SlaAlertService;
import com.prodapt.network_ticketing.service.EmailService;
import com.prodapt.network_ticketing.entity.enums.SlaStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SlaAlertServiceImpl implements SlaAlertService {

    private static final Logger log =
            LoggerFactory.getLogger(SlaAlertServiceImpl.class);

    private final EmailService emailService;

    public SlaAlertServiceImpl(EmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    public void sendSlaBreachAlert(Ticket ticket) {

        log.error(
                "🚨 SLA BREACHED | TicketId={} | Category={} | Priority={} | DueTime={}",
                ticket.getTicketId(),
                ticket.getIssueCategory().getCategoryName(),
                ticket.getPriority(),
                ticket.getSlaDueTime()
        );

        try {
            // notify manager and assigned engineer only for SLA breach
            emailService.sendSlaNotification(ticket, null, SlaStatus.BREACHED, false);
        } catch (Exception ex) {
            log.error("Failed to send SLA breach email: {}", ex.getMessage());
        }
    }
}
