package com.supportai.dto;

public record DocumentChunkMatchResponse(
        Long chunkId,
        Long documentId,
        String documentTitle,
        String content,
        int chunkIndex,
        double score
) {}
