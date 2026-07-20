package com.supportai.dto;

public record TicketStatusBreakdown(
        long open,
        long inProgress,
        long resolved,
        long closed
) {}
