package com.supportai.dto;

import java.time.Instant;

public record CompanyResponse(
        Long id,
        String name,
        String slug,
        String website,
        String aiSystemPrompt,
        Instant createdAt
) {}
