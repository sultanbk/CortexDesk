package com.prodapt.network_ticketing.service.impl;

import com.prodapt.network_ticketing.entity.Ticket;
import com.prodapt.network_ticketing.entity.User;
import com.prodapt.network_ticketing.entity.enums.SlaStatus;
import com.prodapt.network_ticketing.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class EmailServiceImpl implements EmailService {

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:no-reply@networkticketing.local}")
    private String fromAddress;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    @Override
    public void sendSlaNotification(Ticket ticket, SlaStatus oldStatus, SlaStatus newStatus, boolean includeCustomer) {
        if (ticket == null) return;

        String ref = ticket.getTicketReference() != null ? ticket.getTicketReference() : "T-" + ticket.getTicketId();
        String subject = "🚨 SLA Update: " + ref + " is now " + newStatus;

        StringBuilder body = new StringBuilder();
        body.append("Ticket: ").append(ref).append("\n");
        body.append("SLA Status: ").append(newStatus).append("\n");
        if (oldStatus != null) body.append("Previous SLA status: ").append(oldStatus).append("\n");
        body.append("Description: \n").append(ticket.getDescription()).append("\n\n");
        body.append("SLA Due: ").append(ticket.getSlaDueTime()).append("\n");

        // Build recipient list: assigned engineer and manager (always), optionally include customer
        List<String> recipients = new ArrayList<>();
        if (ticket.getAssignedEngineer() != null && ticket.getAssignedEngineer().getEmail() != null && !ticket.getAssignedEngineer().getEmail().isBlank()) {
            recipients.add(ticket.getAssignedEngineer().getEmail());
        }
        if (ticket.getAssignedByManager() != null && ticket.getAssignedByManager().getEmail() != null && !ticket.getAssignedByManager().getEmail().isBlank()) {
            recipients.add(ticket.getAssignedByManager().getEmail());
        }
        if (includeCustomer) {
            User customer = ticket.getCustomer();
            if (customer != null && customer.getEmail() != null && !customer.getEmail().isBlank()) {
                recipients.add(customer.getEmail());
            }
        }

        if (recipients.isEmpty()) {
            return;
        }

        for (String to : recipients) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setFrom(fromAddress);
                msg.setTo(to);
                msg.setSubject(subject);
                msg.setText(body.toString());
                mailSender.send(msg);

                // Log successful email sending
                String timestamp = LocalDateTime.now().format(dateFormatter);
                System.out.println("✅ Email sent successfully [" + timestamp + "] to: " + to);

            } catch (Exception ex) {
                System.err.println("❌ Failed to send SLA notification to " + to + ": " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    @Async
    @Override
    public void sendSimpleEmail(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            System.out.println("⚠️ Email recipient is empty, skipping email");
            return;
        }

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);

            // Log successful email sending
            String timestamp = LocalDateTime.now().format(dateFormatter);
            System.out.println("✅ Email sent successfully [" + timestamp + "] to: " + to);

        } catch (Exception ex) {
            System.err.println("❌ Failed to send email to " + to + ": " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Async
    @Override
    public void sendTicketAssignmentEmail(Ticket ticket) {
        if (ticket == null) return;

        String ref = ticket.getTicketReference() != null ? ticket.getTicketReference() : "T-" + ticket.getTicketId();
        String subject = "🎯 Ticket Assigned: " + ref;

        StringBuilder body = new StringBuilder();
        body.append("Ticket has been successfully assigned.\n\n");
        body.append("─────────────────────────────────────\n");
        body.append("TICKET DETAILS:\n");
        body.append("─────────────────────────────────────\n");
        body.append("Ticket ID: ").append(ref).append("\n");
        body.append("Description: ").append(ticket.getDescription()).append("\n");
        body.append("Category: ").append(ticket.getIssueCategory().getCategoryName()).append("\n");
        body.append("Priority: ").append(ticket.getPriority()).append("\n");

        body.append("\n─────────────────────────────────────\n");
        body.append("SLA INFORMATION:\n");
        body.append("─────────────────────────────────────\n");
        body.append("SLA Start Time: ").append(ticket.getSlaStartTime()).append("\n");
        body.append("SLA Due Time: ").append(ticket.getSlaDueTime()).append("\n");

        long slaMinutes = java.time.temporal.ChronoUnit.MINUTES.between(ticket.getSlaStartTime(), ticket.getSlaDueTime());
        long slaHours = slaMinutes / 60;
        long slaRemainingMinutes = slaMinutes % 60;
        body.append("SLA Duration: ").append(slaHours).append(" hours ").append(slaRemainingMinutes).append(" minutes\n");

        body.append("\n─────────────────────────────────────\n");
        body.append("ASSIGNMENT DETAILS:\n");
        body.append("─────────────────────────────────────\n");

        if (ticket.getAssignedEngineer() != null) {
            body.append("Assigned Engineer: ").append(ticket.getAssignedEngineer().getUsername()).append("\n");
            body.append("Engineer Email: ").append(ticket.getAssignedEngineer().getEmail()).append("\n");
        }

        if (ticket.getAssignedByManager() != null) {
            body.append("Manager: ").append(ticket.getAssignedByManager().getUsername()).append("\n");
            body.append("Manager Email: ").append(ticket.getAssignedByManager().getEmail()).append("\n");
        }

        body.append("\n─────────────────────────────────────\n");
        body.append("Please ensure the ticket is resolved before the SLA due time.\n");
        body.append("─────────────────────────────────────\n");

        List<String> recipients = new ArrayList<>();
        if (ticket.getAssignedEngineer() != null && ticket.getAssignedEngineer().getEmail() != null && !ticket.getAssignedEngineer().getEmail().isBlank()) {
            recipients.add(ticket.getAssignedEngineer().getEmail());
        }
        if (ticket.getAssignedByManager() != null && ticket.getAssignedByManager().getEmail() != null && !ticket.getAssignedByManager().getEmail().isBlank()) {
            recipients.add(ticket.getAssignedByManager().getEmail());
        }

        if (recipients.isEmpty()) {
            return;
        }

        for (String to : recipients) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setFrom(fromAddress);
                msg.setTo(to);
                msg.setSubject(subject);
                msg.setText(body.toString());
                mailSender.send(msg);

                String timestamp = LocalDateTime.now().format(dateFormatter);
                System.out.println("✅ Ticket Assignment Email sent [" + timestamp + "] to: " + to);

            } catch (Exception ex) {
                System.err.println("❌ Failed to send assignment email to " + to + ": " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    @Async
    @Override
    public void sendSlaWarningEmail(Ticket ticket) {
        if (ticket == null) return;

        String ref = ticket.getTicketReference() != null ? ticket.getTicketReference() : "T-" + ticket.getTicketId();
        String subject = "⚠️ SLA 80% Alert: " + ref + " - Urgent Action Required";

        StringBuilder body = new StringBuilder();
        body.append("ALERT: 80% of your SLA time has been consumed!\n\n");
        body.append("─────────────────────────────────────\n");
        body.append("TICKET DETAILS:\n");
        body.append("─────────────────────────────────────\n");
        body.append("Ticket ID: ").append(ref).append("\n");
        body.append("Description: ").append(ticket.getDescription()).append("\n");
        body.append("Status: ").append(ticket.getStatus()).append("\n");

        body.append("\n─────────────────────────────────────\n");
        body.append("SLA TIME REMAINING:\n");
        body.append("─────────────────────────────────────\n");

        LocalDateTime now = LocalDateTime.now();
        long remainingMinutes = java.time.temporal.ChronoUnit.MINUTES.between(now, ticket.getSlaDueTime());
        long remainingHours = remainingMinutes / 60;
        long remainingMins = remainingMinutes % 60;

        body.append("Time Remaining: ").append(remainingHours).append(" hours ").append(remainingMins).append(" minutes\n");
        body.append("SLA Due: ").append(ticket.getSlaDueTime()).append("\n");

        body.append("\n─────────────────────────────────────\n");
        body.append("⚠️ IMMEDIATE ACTION REQUIRED\n");
        body.append("─────────────────────────────────────\n");
        body.append("Please expedite the resolution of this ticket to avoid SLA breach.\n");
        body.append("Contact your team members if additional support is needed.\n");

        List<String> recipients = new ArrayList<>();
        if (ticket.getAssignedEngineer() != null && ticket.getAssignedEngineer().getEmail() != null && !ticket.getAssignedEngineer().getEmail().isBlank()) {
            recipients.add(ticket.getAssignedEngineer().getEmail());
        }
        if (ticket.getAssignedByManager() != null && ticket.getAssignedByManager().getEmail() != null && !ticket.getAssignedByManager().getEmail().isBlank()) {
            recipients.add(ticket.getAssignedByManager().getEmail());
        }

        if (recipients.isEmpty()) {
            return;
        }

        for (String to : recipients) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setFrom(fromAddress);
                msg.setTo(to);
                msg.setSubject(subject);
                msg.setText(body.toString());
                mailSender.send(msg);

                String timestamp = LocalDateTime.now().format(dateFormatter);
                System.out.println("✅ SLA Warning Email sent [" + timestamp + "] to: " + to);

            } catch (Exception ex) {
                System.err.println("❌ Failed to send SLA warning email to " + to + ": " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    @Async
    @Override
    public void sendSlaBreachEmail(Ticket ticket) {
        if (ticket == null) return;

        String ref = ticket.getTicketReference() != null ? ticket.getTicketReference() : "T-" + ticket.getTicketId();
        String subject = "🚨 CRITICAL: SLA Breached - " + ref;

        StringBuilder body = new StringBuilder();
        body.append("⚠️ CRITICAL ALERT: SLA HAS BEEN BREACHED!\n\n");
        body.append("─────────────────────────────────────\n");
        body.append("TICKET DETAILS:\n");
        body.append("─────────────────────────────────────\n");
        body.append("Ticket ID: ").append(ref).append("\n");
        body.append("Description: ").append(ticket.getDescription()).append("\n");
        body.append("Customer: ").append(ticket.getCustomer().getUsername()).append("\n");
        body.append("Status: ").append(ticket.getStatus()).append("\n");

        body.append("\n─────────────────────────────────────\n");
        body.append("SLA BREACH INFORMATION:\n");
        body.append("─────────────────────────────────────\n");
        body.append("SLA Due Time: ").append(ticket.getSlaDueTime()).append("\n");
        body.append("Breached At: ").append(LocalDateTime.now()).append("\n");

        LocalDateTime now = LocalDateTime.now();
        long minutesPastDue = java.time.temporal.ChronoUnit.MINUTES.between(ticket.getSlaDueTime(), now);
        long hoursPastDue = minutesPastDue / 60;
        long minsPastDue = minutesPastDue % 60;

        body.append("Time Past Due: ").append(hoursPastDue).append(" hours ").append(minsPastDue).append(" minutes\n");

        body.append("\n─────────────────────────────────────\n");
        body.append("ASSIGNMENT:\n");
        body.append("─────────────────────────────────────\n");

        if (ticket.getAssignedEngineer() != null) {
            body.append("Assigned Engineer: ").append(ticket.getAssignedEngineer().getUsername()).append("\n");
        }

        if (ticket.getAssignedByManager() != null) {
            body.append("Manager: ").append(ticket.getAssignedByManager().getUsername()).append("\n");
        }

        body.append("\n─────────────────────────────────────\n");
        body.append("IMMEDIATE ACTION REQUIRED\n");
        body.append("─────────────────────────────────────\n");
        body.append("This ticket requires immediate attention and escalation.\n");
        body.append("Contact the assigned team immediately.\n");

        List<String> recipients = new ArrayList<>();

        // Add Manager
        if (ticket.getAssignedByManager() != null && ticket.getAssignedByManager().getEmail() != null && !ticket.getAssignedByManager().getEmail().isBlank()) {
            recipients.add(ticket.getAssignedByManager().getEmail());
        }

        // Add Engineer
        if (ticket.getAssignedEngineer() != null && ticket.getAssignedEngineer().getEmail() != null && !ticket.getAssignedEngineer().getEmail().isBlank()) {
            recipients.add(ticket.getAssignedEngineer().getEmail());
        }

        // Add Customer
        if (ticket.getCustomer() != null && ticket.getCustomer().getEmail() != null && !ticket.getCustomer().getEmail().isBlank()) {
            recipients.add(ticket.getCustomer().getEmail());
        }

        if (recipients.isEmpty()) {
            return;
        }

        for (String to : recipients) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setFrom(fromAddress);
                msg.setTo(to);
                msg.setSubject(subject);
                msg.setText(body.toString());
                mailSender.send(msg);

                String timestamp = LocalDateTime.now().format(dateFormatter);
                System.out.println("✅ SLA Breach Email sent [" + timestamp + "] to: " + to);

            } catch (Exception ex) {
                System.err.println("❌ Failed to send SLA breach email to " + to + ": " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    @Async
    @Override
    public void sendTicketCompletionEmail(Ticket ticket) {
        if (ticket == null || ticket.getCustomer() == null) return;

        String customerEmail = ticket.getCustomer().getEmail();
        if (customerEmail == null || customerEmail.isBlank()) {
            System.out.println("⚠️ Customer email is empty, skipping completion email");
            return;
        }

        String ref = ticket.getTicketReference() != null ? ticket.getTicketReference() : "T-" + ticket.getTicketId();
        String subject = "✅ Ticket Resolved: " + ref + " - Issue Completed";

        StringBuilder body = new StringBuilder();
        body.append("Your ticket has been successfully resolved!\n\n");
        body.append("─────────────────────────────────────\n");
        body.append("TICKET SUMMARY:\n");
        body.append("─────────────────────────────────────\n");
        body.append("Ticket ID: ").append(ref).append("\n");
        body.append("Description: ").append(ticket.getDescription()).append("\n");
        body.append("Category: ").append(ticket.getIssueCategory().getCategoryName()).append("\n");

        body.append("\n─────────────────────────────────────\n");
        body.append("RESOLUTION DETAILS:\n");
        body.append("─────────────────────────────────────\n");
        body.append("Status: RESOLVED\n");

        if (ticket.getResolutionSummary() != null && !ticket.getResolutionSummary().isBlank()) {
            body.append("Resolution Summary:\n").append(ticket.getResolutionSummary()).append("\n");
        }

        body.append("\n─────────────────────────────────────\n");
        body.append("TIMELINE:\n");
        body.append("─────────────────────────────────────\n");
        body.append("Created: ").append(ticket.getCreatedAt()).append("\n");
        body.append("Resolved: ").append(ticket.getLastUpdatedAt()).append("\n");

        if (ticket.getClosedAt() != null) {
            body.append("Closed: ").append(ticket.getClosedAt()).append("\n");
        }

        body.append("\n─────────────────────────────────────\n");
        body.append("Thank you for contacting us.\n");
        body.append("If you have any further questions, please feel free to reopen this ticket.\n");
        body.append("─────────────────────────────────────\n");

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(customerEmail);
            msg.setSubject(subject);
            msg.setText(body.toString());
            mailSender.send(msg);

            String timestamp = LocalDateTime.now().format(dateFormatter);
            System.out.println("✅ Ticket Completion Email sent [" + timestamp + "] to: " + customerEmail);

        } catch (Exception ex) {
            System.err.println("❌ Failed to send completion email to " + customerEmail + ": " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
