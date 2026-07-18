package com.supportai.dto;

import com.supportai.enums.DocumentType;

import java.time.Instant;

public record DocumentResponse(
        Long id,
        String title,
        String filename,
        DocumentType type,
        String fileUrl,
        boolean processed,
        Instant createdAt
) {}
