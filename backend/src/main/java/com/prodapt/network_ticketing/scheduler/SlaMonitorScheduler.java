package com.prodapt.network_ticketing.scheduler;

import com.prodapt.network_ticketing.entity.Ticket;
import com.prodapt.network_ticketing.entity.enums.SlaStatus;
import com.prodapt.network_ticketing.entity.enums.TicketStatus;
import com.prodapt.network_ticketing.repository.TicketRepository;
import com.prodapt.network_ticketing.service.SlaAlertService;
import com.prodapt.network_ticketing.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class SlaMonitorScheduler {

    private final TicketRepository ticketRepository;
    private static final Logger log = LoggerFactory.getLogger(SlaMonitorScheduler.class);
    private final SlaAlertService slaAlertService;
    private final EmailService emailService;

    public SlaMonitorScheduler(TicketRepository ticketRepository, SlaAlertService slaAlertService, EmailService emailService) {
        this.ticketRepository = ticketRepository;
        this.slaAlertService = slaAlertService;
        this.emailService = emailService;
    }

    // 🔁 Runs every 5 minutes
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void monitorSlaBreaches() {

        // Fetch only active tickets
        List<Ticket> activeTickets =
                ticketRepository.findByStatusNot(TicketStatus.CLOSED);

        log.info("SLA Scheduler running at {}", LocalDateTime.now());

        LocalDateTime now = LocalDateTime.now();

        for (Ticket ticket : activeTickets) {

            SlaStatus oldStatus = ticket.getSlaStatus();
            SlaStatus newStatus = calculateSlaStatus(ticket, now);

            // 🔁 Update only if SLA status changed
            if (oldStatus != newStatus) {

                ticket.setSlaStatus(newStatus);

                // 🚨 SLA breach alert (ONE TIME ONLY)
                if (newStatus == SlaStatus.BREACHED && !ticket.isSlaAlertSent()) {

                    slaAlertService.sendSlaBreachAlert(ticket);
                    ticket.setSlaAlertSent(true);

                    // Send SLA breach email notification
                    try {
                        emailService.sendSlaBreachEmail(ticket);
                    } catch (Exception ex) {
                        log.error("Error sending SLA breach email for ticket {}: {}", ticket.getTicketId(), ex.getMessage());
                    }
                }

                // 80% SLA warning (when transitioning to AT_RISK)
                if (newStatus == SlaStatus.AT_RISK && oldStatus != SlaStatus.AT_RISK) {
                    try {
                        emailService.sendSlaWarningEmail(ticket);
                    } catch (Exception ex) {
                        log.error("Error sending SLA warning email for ticket {}: {}", ticket.getTicketId(), ex.getMessage());
                    }
                }

                ticketRepository.save(ticket);
            }
        }
    }


    // 🧠 SLA calculation logic
    private SlaStatus calculateSlaStatus(Ticket ticket, LocalDateTime now) {

        if (now.isAfter(ticket.getSlaDueTime())) {
            return SlaStatus.BREACHED;
        }

        long totalMinutes = Duration
                .between(ticket.getSlaStartTime(), ticket.getSlaDueTime())
                .toMinutes();

        long usedMinutes = Duration
                .between(ticket.getSlaStartTime(), now)
                .toMinutes();

        double usageRatio = (double) usedMinutes / totalMinutes;

        if (usageRatio >= 0.8) {
            return SlaStatus.AT_RISK;
        }

        return SlaStatus.ON_TRACK;
    }
}
