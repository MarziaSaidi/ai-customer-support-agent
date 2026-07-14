package com.supportai.dto;

import java.time.Instant;
import java.util.List;

public record ChatSessionResponse(
        Long id,
        String status,
        String customerEmail,
        String customerName,
        boolean resolved,
        Instant createdAt,
        List<MessageResponse> messages
) {}
