package com.supportai.dto;

import java.util.List;

public record AnalyticsResponse(
        long totalConversations,
        long resolvedConversations,
        long openTickets,
        long resolvedTickets,
        double aiResolutionRate,
        double averageResponseTimeMs,
        double customerSatisfaction,
        List<TrendPointResponse> conversationTrend,
        List<QuestionStatResponse> topQuestions,
        TicketStatusBreakdown ticketStatusBreakdown
) {}
