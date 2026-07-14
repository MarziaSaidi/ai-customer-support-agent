package com.supportai.dto;

import java.time.Instant;

public record MessageResponse(
        Long id,
        String role,
        String content,
        Instant createdAt
) {}
