package com.prodapt.network_ticketing.service.impl;

import com.prodapt.network_ticketing.entity.Ticket;
import com.prodapt.network_ticketing.entity.TicketAttachment;
import com.prodapt.network_ticketing.repository.TicketAttachmentRepository;
import com.prodapt.network_ticketing.repository.TicketRepository;
import com.prodapt.network_ticketing.service.TicketAttachmentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
public class TicketAttachmentServiceImpl implements TicketAttachmentService {

    private final TicketAttachmentRepository attachmentRepository;
    private final TicketRepository ticketRepository;

    private final Path uploadRoot;

    public TicketAttachmentServiceImpl(TicketAttachmentRepository attachmentRepository, TicketRepository ticketRepository,
                                       @Value("${app.upload.dir:uploads}") String uploadDir) {
        this.attachmentRepository = attachmentRepository;
        this.ticketRepository = ticketRepository;
        this.uploadRoot = Path.of(uploadDir);
        try { Files.createDirectories(this.uploadRoot); } catch (IOException ignored) {}
    }

    @Override
    public TicketAttachment saveAttachment(Long ticketId, MultipartFile file) throws IOException {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new RuntimeException("Ticket not found"));
        String fname = System.currentTimeMillis() + "-" + file.getOriginalFilename();
        Path dest = uploadRoot.resolve(fname);
        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

        TicketAttachment att = new TicketAttachment();
        att.setTicket(ticket);
        att.setFileName(file.getOriginalFilename());
        att.setContentType(file.getContentType());
        att.setPath(dest.toString());
        return attachmentRepository.save(att);
    }

    @Override
    public List<TicketAttachment> listForTicket(Long ticketId) {
        return attachmentRepository.findByTicketTicketId(ticketId);
    }

    @Override
    public TicketAttachment getAttachment(Long id) {
        return attachmentRepository.findById(id).orElseThrow(() -> new RuntimeException("Attachment not found"));
    }
}
