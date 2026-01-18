package com.prodapt.network_ticketing.service.impl;

import com.prodapt.network_ticketing.dto.CreateTicketRequest;
import com.prodapt.network_ticketing.entity.IssueCategory;
import com.prodapt.network_ticketing.entity.Ticket;
import com.prodapt.network_ticketing.entity.TicketStatusHistory;
import com.prodapt.network_ticketing.entity.User;
import com.prodapt.network_ticketing.entity.enums.Priority;
import com.prodapt.network_ticketing.entity.enums.RoleName;
import com.prodapt.network_ticketing.entity.enums.SlaStatus;
import com.prodapt.network_ticketing.entity.enums.TicketStatus;
import com.prodapt.network_ticketing.repository.IssueCategoryRepository;
import com.prodapt.network_ticketing.repository.TicketRepository;
import com.prodapt.network_ticketing.repository.TicketStatusHistoryRepository;
import com.prodapt.network_ticketing.repository.UserRepository;
import com.prodapt.network_ticketing.service.TicketService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final IssueCategoryRepository issueCategoryRepository;
    private final TicketStatusHistoryRepository historyRepository;
    private final com.prodapt.network_ticketing.service.EmailService emailService;

    public TicketServiceImpl(
            TicketRepository ticketRepository,
            UserRepository userRepository,
            IssueCategoryRepository issueCategoryRepository,
            TicketStatusHistoryRepository historyRepository,
            com.prodapt.network_ticketing.service.EmailService emailService) {

        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.issueCategoryRepository = issueCategoryRepository;
        this.historyRepository = historyRepository;
        this.emailService = emailService;
    }

    // ================= CREATE TICKET =================

    @Override
    public Ticket createTicket(CreateTicketRequest request) {

        User customer = userRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        IssueCategory category = null;
        if (request.getIssueCategoryId() != null) {
            category = issueCategoryRepository.findById(request.getIssueCategoryId())
                    .orElseThrow(() -> new RuntimeException("Issue category not found"));
        } else {
            // attempt simple auto-categorization by keywords
            category = autoCategorize(request.getDescription());
        }

        if (category == null) {
            throw new RuntimeException("Could not determine issue category for ticket");
        }

        Ticket ticket = new Ticket();
        ticket.setCustomer(customer);
        ticket.setIssueCategory(category);
        ticket.setDescription(request.getDescription());
        ticket.setStatus(TicketStatus.NEW);
        ticket.setSlaStatus(SlaStatus.ON_TRACK);

        // Set initial SLA placeholders (DB requires non-null); actual SLA countdown
        // will be considered started only after manager assignment.
        LocalDateTime now = LocalDateTime.now();
        ticket.setSlaStartTime(now);
        long slaHours = 168; // default to 7 days
        if (category.getSlaHours() != null) {
            slaHours = category.getSlaHours();
        }
        ticket.setSlaDueTime(now.plusHours(slaHours));

        Ticket savedTicket = ticketRepository.save(ticket);

        // generate human-friendly ticket reference after we have the DB id
        String ref = generateTicketReference(savedTicket.getTicketId());
        savedTicket.setTicketReference(ref);
        savedTicket = ticketRepository.save(savedTicket);

        logStatusChange(savedTicket, null, TicketStatus.NEW, customer);
        return savedTicket;
    }

    // generate T-000123 style reference
    private String generateTicketReference(Long id) {
        if (id == null) return null;
        return String.format("T-%06d", id);
    }

    // simple keyword matcher similar to frontend autoCategorize
    private IssueCategory autoCategorize(String description) {
        if (description == null || description.trim().length() < 4) return null;
        String norm = description.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
        String[] tokens = norm.split("\\s+");
        List<IssueCategory> cats = issueCategoryRepository.findAll();
        IssueCategory best = null;
        double bestScore = 0;
        for (IssueCategory c : cats) {
            String pool = ((c.getCategoryName() == null ? "" : c.getCategoryName()) + " " + (c.getDescription() == null ? "" : c.getDescription())).toLowerCase();
            double score = 0;
            for (String t : tokens) {
                if (t.length() <= 2) continue;
                if (pool.contains(t)) score += 2;
                // partial matches
                String[] poolTokens = pool.split("\\s+");
                for (String p : poolTokens) {
                    if (p.contains(t) || t.contains(p)) score += 0.5;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                best = c;
            }
        }
        return bestScore >= 1 ? best : null;
    }

    // ================= ENGINEER SELF PICK =================

    @Transactional
    @Override
    public Ticket pickTicket(Long ticketId, Long engineerId) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        // Allow picking NEW or ASSIGNED tickets
        if (ticket.getStatus() != TicketStatus.NEW && ticket.getStatus() != TicketStatus.ASSIGNED) {
            throw new RuntimeException("Only NEW or ASSIGNED tickets can be picked");
        }

        User engineer = userRepository.findById(engineerId)
                .orElseThrow(() -> new RuntimeException("Engineer not found"));

        if (engineer.getRole().getRoleName() != RoleName.ENGINEER) {
            throw new RuntimeException("User is not an engineer");
        }

        TicketStatus oldStatus = ticket.getStatus();

        // If ticket was previously assigned to a specific engineer, ensure it matches
        if (oldStatus == TicketStatus.ASSIGNED && ticket.getAssignedEngineer() != null && !ticket.getAssignedEngineer().getUserId().equals(engineerId)) {
            throw new RuntimeException("Ticket already assigned to another engineer");
        }

        // assign engineer if not already set
        if (ticket.getAssignedEngineer() == null) ticket.setAssignedEngineer(engineer);

        ticket.setStatus(TicketStatus.IN_PROGRESS);

        // If this ticket was not manager-assigned, starting work by engineer should
        // start the SLA countdown now.
        if (ticket.getAssignedByManager() == null) {
            LocalDateTime now = LocalDateTime.now();
            ticket.setSlaStartTime(now);
            long slaHoursAssign = 168;
            if (ticket.getIssueCategory() != null && ticket.getIssueCategory().getSlaHours() != null) slaHoursAssign = ticket.getIssueCategory().getSlaHours();
            ticket.setSlaDueTime(now.plusHours(slaHoursAssign));
            ticket.setSlaStatus(SlaStatus.ON_TRACK);
        }

        logStatusChange(ticket, oldStatus, TicketStatus.IN_PROGRESS, engineer);
        return ticketRepository.save(ticket);
    }

    @Override
    public Ticket autoAssignTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        if (ticket.getStatus() != TicketStatus.NEW) {
            throw new RuntimeException("Only NEW tickets can be auto-assigned");
        }
        // Weighted heuristic: balance engineer load and SLA risk
        List<User> engineers = userRepository.findByRole_RoleName(RoleName.ENGINEER);
        if (engineers.isEmpty()) throw new RuntimeException("No engineers available");

        User best = null;
        double bestScore = Double.MAX_VALUE;

        for (User eng : engineers) {
            List<Ticket> assigned = ticketRepository.findByAssignedEngineer_UserId(eng.getUserId());
            int openCount = 0;
            int riskScore = 0;
            for (Ticket t : assigned) {
                // consider only open tickets
                if (t.getStatus() != TicketStatus.CLOSED && t.getStatus() != TicketStatus.RESOLVED) openCount++;
                if (t.getSlaStatus() == SlaStatus.BREACHED) riskScore += 3;
                else if (t.getSlaStatus() == SlaStatus.AT_RISK) riskScore += 2;
                else if (t.getSlaStatus() == SlaStatus.ON_TRACK) riskScore += 1;
            }

            // engineer base load weight
            double loadWeight = openCount * 2.0;

            // riskWeight increases score (we prefer engineers with lower risk)
            double riskWeight = riskScore * 1.5;

            // priority factor: prefer engineers who currently handle similar priority levels less
            double priorityFactor = 0.0;
            // if engineer has many HIGH priority tickets, increase factor
            int highCount = (int) assigned.stream().filter(tt -> tt.getPriority() == Priority.HIGH).count();
            priorityFactor = highCount * 1.0;

            double score = loadWeight + riskWeight + priorityFactor + Math.random() * 0.1; // tiny jitter to break ties

            System.out.println("AutoAssign: engineer=" + eng.getUsername() + " load=" + openCount + " risk=" + riskScore + " score=" + score);

            if (score < bestScore) {
                bestScore = score;
                best = eng;
            }
        }

        if (best == null) throw new RuntimeException("No engineer selected");

        ticket.setAssignedEngineer(best);
        ticket.setStatus(TicketStatus.ASSIGNED);

        // start SLA when system assigns
        LocalDateTime now = LocalDateTime.now();
        ticket.setSlaStartTime(now);
        long slaHoursAssign = 168;
        if (ticket.getIssueCategory() != null && ticket.getIssueCategory().getSlaHours() != null) slaHoursAssign = ticket.getIssueCategory().getSlaHours();
        ticket.setSlaDueTime(now.plusHours(slaHoursAssign));
        ticket.setSlaStatus(SlaStatus.ON_TRACK);

        logStatusChange(ticket, TicketStatus.NEW, TicketStatus.ASSIGNED, null);
        return ticketRepository.save(ticket);
    }

    @Override
    public List<Ticket> getEngineerQueue(Long engineerId) {
        // tickets available to pick: NEW, or ASSIGNED to this engineer or unassigned ASSIGNED
        List<Ticket> candidates = ticketRepository.findByStatusNot(TicketStatus.CLOSED);

        return candidates.stream()
            .filter(t ->
                // NEW tickets
                t.getStatus() == TicketStatus.NEW
                // ASSIGNED and available to this engineer (unassigned or assigned to them)
                || (t.getStatus() == TicketStatus.ASSIGNED && (t.getAssignedEngineer() == null || t.getAssignedEngineer().getUserId().equals(engineerId)))
                // IN_PROGRESS tickets assigned to this engineer
                || (t.getStatus() == TicketStatus.IN_PROGRESS && t.getAssignedEngineer() != null && t.getAssignedEngineer().getUserId().equals(engineerId))
            )
                .sorted((a, b) -> {
                    // SLA urgency: BREACHED > AT_RISK > ON_TRACK
                    int sa = slaPriority(a.getSlaStatus());
                    int sb = slaPriority(b.getSlaStatus());
                    if (sa != sb) return Integer.compare(sb, sa);
                    // priority: HIGH > MEDIUM > LOW
                    int pa = priorityValue(a.getPriority());
                    int pb = priorityValue(b.getPriority());
                    if (pa != pb) return Integer.compare(pb, pa);
                    return a.getSlaDueTime().compareTo(b.getSlaDueTime());
                })
                .toList();
    }

    private int slaPriority(SlaStatus s) {
        if (s == null) return 0;
        return switch (s) {
            case BREACHED -> 3;
            case AT_RISK -> 2;
            case ON_TRACK -> 1;
            default -> 0;
        };
    }

    private int priorityValue(com.prodapt.network_ticketing.entity.enums.Priority p) {
        if (p == null) return 0;
        return switch (p) {
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }

    // ================= MANAGER ASSIGN =================

    @Transactional
    public Ticket assignTicket(Long ticketId, Long engineerId, Long managerId, String priority) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Manager not found"));

        if (manager.getRole().getRoleName() != RoleName.MANAGER) {
            throw new RuntimeException("User is not a manager");
        }

        User engineer = userRepository.findById(engineerId)
                .orElseThrow(() -> new RuntimeException("Engineer not found"));

        if (engineer.getRole().getRoleName() != RoleName.ENGINEER) {
            throw new RuntimeException("User is not an engineer");
        }

        TicketStatus oldStatus = ticket.getStatus();

        ticket.setAssignedEngineer(engineer);
        ticket.setAssignedByManager(manager);
        ticket.setPriority(Priority.valueOf(priority));

        // Set to ASSIGNED (manager assigned but work not started)
        ticket.setStatus(TicketStatus.ASSIGNED);

        // Start/Reset SLA when manager assigns the ticket
        LocalDateTime now = LocalDateTime.now();
        ticket.setSlaStartTime(now);
        long slaHoursAssign = 168;
        if (ticket.getIssueCategory() != null && ticket.getIssueCategory().getSlaHours() != null) {
            slaHoursAssign = ticket.getIssueCategory().getSlaHours();
        }
        ticket.setSlaDueTime(now.plusHours(slaHoursAssign));
        ticket.setSlaStatus(SlaStatus.ON_TRACK);

        logStatusChange(ticket, oldStatus, TicketStatus.ASSIGNED, manager);

        Ticket savedTicket = ticketRepository.save(ticket);

        // Send ticket assignment email
        try {
            emailService.sendTicketAssignmentEmail(savedTicket);
        } catch (Exception ex) {
            System.err.println("Error sending assignment email: " + ex.getMessage());
        }

        return savedTicket;
    }


    // ================= ENGINEER RESOLVE =================

    @Transactional
    @Override
    public Ticket resolveTicket(Long ticketId, Long engineerId, String resolutionSummary) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        if (ticket.getStatus() != TicketStatus.IN_PROGRESS) {
            throw new RuntimeException("Only IN_PROGRESS tickets can be resolved");
        }

        User engineer = userRepository.findById(engineerId)
                .orElseThrow(() -> new RuntimeException("Engineer not found"));

        if (engineer.getRole().getRoleName() != RoleName.ENGINEER) {
            throw new RuntimeException("User is not an engineer");
        }

        if (ticket.getAssignedEngineer() == null ||
                !ticket.getAssignedEngineer().getUserId().equals(engineerId)) {
            throw new RuntimeException("Engineer is not assigned to this ticket");
        }

        if (resolutionSummary == null || resolutionSummary.isBlank()) {
            throw new RuntimeException("Resolution summary is required");
        }

        TicketStatus oldStatus = ticket.getStatus();

        ticket.setResolutionSummary(resolutionSummary);
        ticket.setStatus(TicketStatus.RESOLVED);

        logStatusChange(ticket, oldStatus, TicketStatus.RESOLVED, engineer);
        Ticket saved = ticketRepository.save(ticket);

        // Notify customer when ticket is resolved
        try {
            if (saved.getCustomer() != null && saved.getCustomer().getEmail() != null && !saved.getCustomer().getEmail().isBlank()) {
                String to = saved.getCustomer().getEmail();
                String subject = "Ticket " + (saved.getTicketReference() != null ? saved.getTicketReference() : ("T-" + saved.getTicketId())) + " - Resolved";
                StringBuilder body = new StringBuilder();
                body.append("Your ticket has been resolved.\n\n");
                body.append("Resolution Summary:\n").append(saved.getResolutionSummary()).append("\n\n");
                body.append("If you are satisfied, please close the ticket.\n");
                emailService.sendSimpleEmail(to, subject, body.toString());
            }
        } catch (Exception ex) {
            System.err.println("Failed to send resolution email: " + ex.getMessage());
        }

        return saved;
    }


    // ================= CUSTOMER CLOSE =================

    @Transactional
    @Override
    public Ticket closeTicket(Long ticketId, Long customerId) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (customer.getRole().getRoleName() != RoleName.CUSTOMER) {
            throw new RuntimeException("Only customer can close the ticket");
        }

        if (ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new RuntimeException("Only resolved tickets can be closed");
        }

        ticket.setStatus(TicketStatus.CLOSED);
        ticket.setClosedBy("CUSTOMER");
        ticket.setClosedAt(LocalDateTime.now());

        Ticket savedTicket = ticketRepository.save(ticket);

        // Send ticket completion email to customer
        try {
            emailService.sendTicketCompletionEmail(savedTicket);
        } catch (Exception ex) {
            System.err.println("Error sending completion email: " + ex.getMessage());
        }

        return savedTicket;
    }


    // ================= CUSTOMER REOPEN =================

    @Transactional
    @Override
    public Ticket reopenTicket(Long ticketId, Long customerId, String reopenReason) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        // ✅ Only RESOLVED tickets can be reopened
        if (ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new RuntimeException("Only RESOLVED tickets can be reopened");
        }

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (customer.getRole().getRoleName() != RoleName.CUSTOMER) {
            throw new RuntimeException("User is not a customer");
        }

        if (!ticket.getCustomer().getUserId().equals(customerId)) {
            throw new RuntimeException("Customer is not owner of this ticket");
        }

        if (reopenReason == null || reopenReason.isBlank()) {
            throw new RuntimeException("Reopen reason is required");
        }

        TicketStatus oldStatus = ticket.getStatus();

        ticket.setStatus(TicketStatus.REOPENED);
        ticket.setResolutionSummary(
                "[REOPENED BY CUSTOMER]\nReason: " + reopenReason
        );

        logStatusChange(ticket, oldStatus, TicketStatus.REOPENED, customer);

        return ticketRepository.save(ticket);
    }

    // ================= QUERIES =================

    @Override
    public List<Ticket> getTicketsForCustomer(Long customerId) {
        List<Ticket> tickets = ticketRepository.findByCustomer_UserId(customerId);
        tickets.forEach(this::refreshSlaStatusIfRequired);
        return tickets;
    }

    @Override
    public List<Ticket> getTicketsForEngineer(Long engineerId) {
        List<Ticket> tickets = ticketRepository.findByAssignedEngineer_UserId(engineerId);
        tickets.forEach(this::refreshSlaStatusIfRequired);
        return tickets;
    }

    @Override
    public List<Ticket> getAllTickets() {
        List<Ticket> tickets = ticketRepository.findAll();
        tickets.forEach(this::refreshSlaStatusIfRequired);
        return tickets;
    }

    @Override
    public List<TicketStatusHistory> getTicketHistory(Long ticketId) {
        return historyRepository.findByTicket_TicketId(ticketId);
    }

    // ================= HELPER =================

    private void logStatusChange(
            Ticket ticket,
            TicketStatus oldStatus,
            TicketStatus newStatus,
            User changedBy) {

        TicketStatusHistory history = new TicketStatusHistory();
        history.setTicket(ticket);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setChangedBy(changedBy);

        historyRepository.save(history);
    }
    private SlaStatus calculateSlaStatus(Ticket ticket) {

        LocalDateTime now = LocalDateTime.now();

        // If SLA has not been started by manager assignment, consider it ON_TRACK
        if (ticket.getAssignedByManager() == null) {
            // If engineer has started work (IN_PROGRESS), treat SLA as started
            if (ticket.getStatus() != TicketStatus.IN_PROGRESS) {
                return SlaStatus.ON_TRACK;
            }
        }

        // Breached
        if (now.isAfter(ticket.getSlaDueTime())) {
            return SlaStatus.BREACHED;
        }

        // Calculate SLA consumption %
        long totalMinutes = java.time.Duration.between(ticket.getSlaStartTime(), ticket.getSlaDueTime()).toMinutes();
        if (totalMinutes <= 0) return SlaStatus.ON_TRACK;

        long usedMinutes = java.time.Duration.between(ticket.getSlaStartTime(), now).toMinutes();

        double usageRatio = (double) usedMinutes / totalMinutes;

        // At risk if 80% SLA time used
        if (usageRatio >= 0.8) {
            return SlaStatus.AT_RISK;
        }

        return SlaStatus.ON_TRACK;
    }
    private void refreshSlaStatusIfRequired(Ticket ticket) {

        // Do not update SLA for closed tickets
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            return;
        }

        SlaStatus oldStatus = ticket.getSlaStatus();
        SlaStatus newStatus = calculateSlaStatus(ticket);

        if (oldStatus != newStatus) {
            ticket.setSlaStatus(newStatus);
            ticketRepository.save(ticket);

            // notify when SLA becomes AT_RISK or BREACHED (manager + engineer only)
            if (newStatus == SlaStatus.AT_RISK || newStatus == SlaStatus.BREACHED) {
                try {
                    emailService.sendSlaNotification(ticket, oldStatus, newStatus, false);
                } catch (Exception ex) {
                    System.err.println("Error sending SLA notification: " + ex.getMessage());
                }
            }
        }
    }
    @Override
    public Ticket updatePriority(Long ticketId, String priorityStr) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        // 🔥 STRING → ENUM conversion (SAFE)
        Priority priority;
        try {
            priority = Priority.valueOf(priorityStr.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Invalid priority value: " + priorityStr);
        }

        ticket.setPriority(priority);
        ticket.setLastUpdatedAt(LocalDateTime.now());

        return ticketRepository.save(ticket);
    }

    @Override
    public Ticket assignEngineer(Long ticketId, Long engineerId) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        User engineer = userRepository.findById(engineerId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        TicketStatus oldStatus = ticket.getStatus();
        ticket.setAssignedEngineer(engineer);
        ticket.setStatus(TicketStatus.ASSIGNED);

        // Start/Reset SLA when engineer is assigned
        LocalDateTime now = LocalDateTime.now();
        ticket.setSlaStartTime(now);
        long slaHoursAssign = 168;
        if (ticket.getIssueCategory() != null && ticket.getIssueCategory().getSlaHours() != null) {
            slaHoursAssign = ticket.getIssueCategory().getSlaHours();
        }
        ticket.setSlaDueTime(now.plusHours(slaHoursAssign));
        ticket.setSlaStatus(SlaStatus.ON_TRACK);

        ticket.setLastUpdatedAt(LocalDateTime.now());
        logStatusChange(ticket, oldStatus, TicketStatus.ASSIGNED, engineer);
        return ticketRepository.save(ticket);
    }

    @Override
    public Ticket setSlaMinutes(Long ticketId, long minutes) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        LocalDateTime now = LocalDateTime.now();
        ticket.setSlaStartTime(now);
        ticket.setSlaDueTime(now.plusMinutes(minutes));
        ticket.setSlaStatus(SlaStatus.ON_TRACK);

        // log an informational history entry
        logStatusChange(ticket, ticket.getStatus(), ticket.getStatus(), null);

        return ticketRepository.save(ticket);
    }

    @Override
    @Transactional
    public Ticket addAiResolution(Long ticketId, String aiResolution) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        ticket.setAiResolution(aiResolution);
        ticket.setLastUpdatedAt(LocalDateTime.now());

        return ticketRepository.save(ticket);
    }
}
