package com.prodapt.network_ticketing.controller;

import com.prodapt.network_ticketing.dto.*;
import com.prodapt.network_ticketing.entity.Ticket;
import com.prodapt.network_ticketing.service.TicketService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<?> createTicket(@RequestBody CreateTicketRequest request) {
        System.out.println("Create Ticket API called");
        System.out.println("Customer ID: " + request.getCustomerId());
        System.out.println("Category ID: " + request.getIssueCategoryId());

        Ticket ticket = ticketService.createTicket(request);
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/pick")
    public ResponseEntity<?> pickTicket(@RequestBody Map<String, Object> body) {
        try {
            // defensive parsing: accept numbers as strings or numbers
            Object tId = body.get("ticketId");
            Object eId = body.get("engineerId");

            Long ticketId = null;
            Long engineerId = null;

            if (tId instanceof Number) ticketId = ((Number) tId).longValue();
            else if (tId instanceof String) ticketId = Long.valueOf((String) tId);

            if (eId instanceof Number) engineerId = ((Number) eId).longValue();
            else if (eId instanceof String) engineerId = Long.valueOf((String) eId);

            if (ticketId == null || engineerId == null) {
                return ResponseEntity.badRequest().body("ticketId and engineerId are required");
            }

            System.out.println("Received pick request - ticketId=" + ticketId + " engineerId=" + engineerId);

            Ticket ticket = ticketService.pickTicket(ticketId, engineerId);
            return ResponseEntity.ok(ticket);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/assign")
    public ResponseEntity<?> assignTicket(@RequestBody AssignTicketRequest request) {
        try {
            Ticket ticket = ticketService.assignTicket(
                    request.getTicketId(),
                    request.getEngineerId(),
                    request.getManagerId(),
                    request.getPriority()
            );
            return ResponseEntity.ok(ticket);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/resolve")
    public ResponseEntity<?> resolveTicket(@RequestBody ResolveTicketRequest request) {
        try {
            // Log what we received to be 100% sure
            System.out.println("Processing Ticket: " + request.getTicketId());

            Ticket ticket = ticketService.resolveTicket(
                    request.getTicketId(),
                    request.getEngineerId(),
                    request.getResolutionSummary()
            );
            return ResponseEntity.ok(ticket);
        } catch (Exception e) {
            // THIS IS THE KEY: Print the stack trace to your Java console!
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
    @PostMapping("/close")
    public ResponseEntity<?> closeTicket(@RequestBody CloseTicketRequest request) {
        try {
            Ticket ticket = ticketService.closeTicket(
                    request.getTicketId(),
                    request.getCustomerId()
            );
            return ResponseEntity.ok(ticket);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/reopen")
    public ResponseEntity<?> reopenTicket(@RequestBody ReopenTicketRequest request) {

        Ticket ticket = ticketService.reopenTicket(
                request.getTicketId(),
                request.getCustomerId(),
                request.getReopenReason()
        );

        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<?> getTicketsForCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(ticketService.getTicketsForCustomer(customerId));
    }
    @GetMapping("/engineer/{engineerId}")
    public ResponseEntity<?> getTicketsForEngineer(@PathVariable Long engineerId) {
        return ResponseEntity.ok(ticketService.getTicketsForEngineer(engineerId));
    }
    @GetMapping("/all")
    public ResponseEntity<?> getAllTickets() {
        return ResponseEntity.ok(ticketService.getAllTickets());
    }
    @GetMapping("/{ticketId}/history")
    public ResponseEntity<?> getTicketHistory(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ticketService.getTicketHistory(ticketId));
    }
    @PutMapping("/{ticketId}/priority")
    public ResponseEntity<Ticket> updatePriority(
            @PathVariable Long ticketId,
            @RequestBody Map<String, String> body
    ) {
        return ResponseEntity.ok(
                ticketService.updatePriority(ticketId, body.get("priority"))
        );
    }

    @PutMapping("/{ticketId}/assign")
    public ResponseEntity<Ticket> assignEngineer(
            @PathVariable Long ticketId,
            @RequestBody Map<String, Long> body
    ) {
        return ResponseEntity.ok(
                ticketService.assignEngineer(ticketId, body.get("engineerId"))
        );
    }

    // DEBUG: set SLA duration in minutes for a ticket (useful for local testing)
    @PostMapping("/{ticketId}/debug/sla")
    public ResponseEntity<?> setSlaMinutes(@PathVariable Long ticketId, @RequestParam long minutes) {
        try {
            Ticket t = ticketService.setSlaMinutes(ticketId, minutes);
            return ResponseEntity.ok(t);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{ticketId}/ai-resolution")
    public ResponseEntity<?> addAiResolution(
            @PathVariable Long ticketId,
            @RequestBody AddAiResolutionRequest request
    ) {
        try {
            Ticket ticket = ticketService.addAiResolution(ticketId, request.getAiResolution());
            return ResponseEntity.ok(ticket);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/{ticketId}/autoassign")
    public ResponseEntity<?> autoAssign(@PathVariable Long ticketId) {
        try {
            Ticket ticket = ticketService.autoAssignTicket(ticketId);
            return ResponseEntity.ok(ticket);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/engineer/{engineerId}/queue")
    public ResponseEntity<?> getEngineerQueue(@PathVariable Long engineerId) {
        return ResponseEntity.ok(ticketService.getEngineerQueue(engineerId));
    }
}
