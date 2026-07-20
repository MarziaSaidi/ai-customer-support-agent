package com.supportai.dto;

import java.time.Instant;

public record ConversationSummaryResponse(
        Long id,
        String status,
        String customerEmail,
        String customerName,
        boolean resolved,
        Instant createdAt,
        Instant updatedAt,
        String lastMessagePreview,
        int messageCount
) {}
