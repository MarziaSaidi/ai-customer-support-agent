package com.supportai.dto;

import com.supportai.entity.Ticket;
import com.supportai.entity.User;
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
        Long assignedToUserId,
        String assignedToName,
        String internalNotes,
        Instant createdAt,
        Instant updatedAt
) {
    public static TicketResponse from(Ticket ticket) {
        User assignee = ticket.getAssignedTo();
        return new TicketResponse(
                ticket.getId(),
                ticket.getCompany().getId(),
                ticket.getConversation() != null ? ticket.getConversation().getId() : null,
                ticket.getSubject(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getCustomerEmail(),
                assignee != null ? assignee.getId() : null,
                assignee != null ? assignee.getFirstName() + " " + assignee.getLastName() : null,
                ticket.getInternalNotes(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}
