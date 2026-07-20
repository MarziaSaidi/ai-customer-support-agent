package com.supportai.dto;

import com.supportai.entity.Ticket;
import com.supportai.enums.TicketPriority;
import com.supportai.enums.TicketStatus;

import java.time.Instant;

public record TicketResponse(
        Long id,
        Long companyId,
        Long conversationId,
        String subject,
        String description,
        TicketStatus status,
        TicketPriority priority,
        String customerEmail,
        Instant createdAt,
        Instant updatedAt
) {
    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getCompany().getId(),
                ticket.getConversation() != null ? ticket.getConversation().getId() : null,
                ticket.getSubject(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getCustomerEmail(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}
