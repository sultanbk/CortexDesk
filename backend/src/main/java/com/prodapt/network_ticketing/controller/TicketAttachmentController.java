package com.prodapt.network_ticketing.controller;

import com.prodapt.network_ticketing.entity.TicketAttachment;
import com.prodapt.network_ticketing.service.TicketAttachmentService;
import org.springframework.core.io.PathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/tickets")
public class TicketAttachmentController {

    private final TicketAttachmentService service;

    public TicketAttachmentController(TicketAttachmentService service) {
        this.service = service;
    }

    @PostMapping("/{ticketId}/attachments")
    public ResponseEntity<?> upload(@PathVariable Long ticketId, @RequestParam("file") MultipartFile file) {
        try {
            TicketAttachment att = service.saveAttachment(ticketId, file);
            return ResponseEntity.ok(att);
        } catch (IOException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @GetMapping("/{ticketId}/attachments")
    public ResponseEntity<List<TicketAttachment>> list(@PathVariable Long ticketId) {
        return ResponseEntity.ok(service.listForTicket(ticketId));
    }

    @GetMapping("/attachments/{id}/download")
    public ResponseEntity<?> download(@PathVariable Long id) {
        TicketAttachment att = service.getAttachment(id);
        PathResource resource = new PathResource(Path.of(att.getPath()));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + att.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(att.getContentType() == null ? "application/octet-stream" : att.getContentType()))
                .body(resource);
    }
}
