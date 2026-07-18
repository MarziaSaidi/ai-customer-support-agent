package com.supportai.dto;

public record RagSourceResponse(
        Long documentId,
        String documentTitle,
        String excerpt,
        double score
) {}
