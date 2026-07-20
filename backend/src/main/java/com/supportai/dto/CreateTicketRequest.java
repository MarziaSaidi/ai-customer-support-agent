package com.supportai.dto;

import com.supportai.enums.TicketPriority;
import jakarta.validation.constraints.NotBlank;

public record CreateTicketRequest(
        @NotBlank String subject,
        String description,
        TicketPriority priority,
        String customerEmail,
        Long conversationId
) {}
