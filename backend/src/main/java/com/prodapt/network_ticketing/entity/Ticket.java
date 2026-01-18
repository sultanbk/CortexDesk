package com.prodapt.network_ticketing.entity;

import com.prodapt.network_ticketing.entity.enums.Priority;
import com.prodapt.network_ticketing.entity.enums.SlaStatus;
import com.prodapt.network_ticketing.entity.enums.TicketStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Long ticketId;

    // 🔹 Relationships to User
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne
    @JoinColumn(name = "assigned_engineer_id")
    private User assignedEngineer;

    @ManyToOne
    @JoinColumn(name = "assigned_by_manager_id")
    private User assignedByManager;

    // 🔹 Issue category
    @ManyToOne
    @JoinColumn(name = "issue_category_id", nullable = false)
    private IssueCategory issueCategory;

    // 🔹 Ticket details
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "ticket_reference", unique = true)
    private String ticketReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private Priority priority;

    // 🔹 SLA fields
    @Column(name = "sla_start_time", nullable = false)
    private LocalDateTime slaStartTime;

    @Column(name = "sla_due_time", nullable = false)
    private LocalDateTime slaDueTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "sla_status")
    private SlaStatus slaStatus;

    // 🔹 Resolution & closure
    @Column(name = "resolution_summary", columnDefinition = "TEXT")
    private String resolutionSummary;

    @Column(name = "ai_resolution", columnDefinition = "TEXT")
    private String aiResolution; // AI chatbot response

    @Column(name = "closed_by")
    private String closedBy; // CUSTOMER / ENGINEER

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    // 🔹 Audit
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;

    @Column(name = "sla_alert_sent")
    private boolean slaAlertSent = false;

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    // 🔹 JPA required constructor
    public Ticket() {
    }

    public boolean isSlaAlertSent() {
        return slaAlertSent;
    }

    public void setSlaAlertSent(boolean slaAlertSent) {
        this.slaAlertSent = slaAlertSent;
    }

    // 🔹 Auto timestamps
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.lastUpdatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastUpdatedAt = LocalDateTime.now();
    }

    // 🔹 Getters & Setters (generate via IDE)
    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public User getCustomer() {
        return customer;
    }

    public void setCustomer(User customer) {
        this.customer = customer;
    }

    public User getAssignedEngineer() {
        return assignedEngineer;
    }

    public void setAssignedEngineer(User assignedEngineer) {
        this.assignedEngineer = assignedEngineer;
    }

    public User getAssignedByManager() {
        return assignedByManager;
    }

    public void setAssignedByManager(User assignedByManager) {
        this.assignedByManager = assignedByManager;
    }

    public IssueCategory getIssueCategory() {
        return issueCategory;
    }

    public void setIssueCategory(IssueCategory issueCategory) {
        this.issueCategory = issueCategory;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTicketReference() {
        return ticketReference;
    }

    public void setTicketReference(String ticketReference) {
        this.ticketReference = ticketReference;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public LocalDateTime getSlaStartTime() {
        return slaStartTime;
    }

    public void setSlaStartTime(LocalDateTime slaStartTime) {
        this.slaStartTime = slaStartTime;
    }

    public LocalDateTime getSlaDueTime() {
        return slaDueTime;
    }

    public void setSlaDueTime(LocalDateTime slaDueTime) {
        this.slaDueTime = slaDueTime;
    }

    public SlaStatus getSlaStatus() {
        return slaStatus;
    }

    public void setSlaStatus(SlaStatus slaStatus) {
        this.slaStatus = slaStatus;
    }

    public String getResolutionSummary() {
        return resolutionSummary;
    }

    public void setResolutionSummary(String resolutionSummary) {
        this.resolutionSummary = resolutionSummary;
    }

    public String getAiResolution() {
        return aiResolution;
    }

    public void setAiResolution(String aiResolution) {
        this.aiResolution = aiResolution;
    }

    public String getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(String closedBy) {
        this.closedBy = closedBy;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }
}
