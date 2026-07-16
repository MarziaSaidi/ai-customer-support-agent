package com.supportai.dto;

import jakarta.validation.constraints.Size;

public record UpdateCompanyRequest(
        @Size(max = 200) String name,
        @Size(max = 500) String website,
        @Size(max = 4000) String aiSystemPrompt
) {}
