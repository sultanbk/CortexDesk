package com.prodapt.network_ticketing.service;

import com.prodapt.network_ticketing.entity.Ticket;
import com.prodapt.network_ticketing.entity.enums.SlaStatus;

public interface EmailService {
    /**
     * Send an SLA notification. If includeCustomer is false, only manager and assigned engineer will be notified.
     */
    void sendSlaNotification(Ticket ticket, SlaStatus oldStatus, SlaStatus newStatus, boolean includeCustomer);

    void sendSimpleEmail(String to, String subject, String body);

    /**
     * Send email when ticket is assigned
     * Recipients: Assigned Engineer, Assigned Manager
     * Content: SLA time, assignment details
     */
    void sendTicketAssignmentEmail(Ticket ticket);

    /**
     * Send email when 80% of SLA is consumed
     * Recipients: Assigned Engineer, Assigned Manager
     */
    void sendSlaWarningEmail(Ticket ticket);

    /**
     * Send email when SLA is breached
     * Recipients: Manager, Engineer, Customer
     */
    void sendSlaBreachEmail(Ticket ticket);

    /**
     * Send email when ticket is completed successfully
     * Recipients: Customer only
     */
    void sendTicketCompletionEmail(Ticket ticket);
}
