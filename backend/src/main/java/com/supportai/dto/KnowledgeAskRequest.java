package com.supportai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record KnowledgeAskRequest(
        @NotNull Long companyId,
        @NotBlank String question
) {}
