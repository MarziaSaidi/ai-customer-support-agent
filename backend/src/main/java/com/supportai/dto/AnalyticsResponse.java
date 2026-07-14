package com.supportai.dto;

public record AnalyticsResponse(
        long totalConversations,
        long openTickets,
        long resolvedTickets,
        double aiResolutionRate,
        double averageResponseTimeMs,
        double customerSatisfaction
) {}
