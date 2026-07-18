package com.supportai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DocumentSearchRequest(
        @NotNull Long companyId,
        @NotBlank String query,
        Integer limit
) {}
