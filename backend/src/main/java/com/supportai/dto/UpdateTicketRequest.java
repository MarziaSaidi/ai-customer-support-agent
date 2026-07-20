package com.supportai.dto;

import com.supportai.enums.TicketPriority;
import com.supportai.enums.TicketStatus;

public record UpdateTicketRequest(
        TicketStatus status,
        TicketPriority priority,
        Long assignedToUserId
) {}
