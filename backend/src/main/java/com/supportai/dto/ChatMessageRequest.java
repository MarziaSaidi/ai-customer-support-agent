package com.supportai.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatMessageRequest(
        @NotBlank String content,
        String customerEmail,
        String customerName
) {}
