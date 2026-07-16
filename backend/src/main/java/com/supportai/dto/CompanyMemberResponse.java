package com.supportai.dto;

import java.time.Instant;

public record CompanyMemberResponse(
        Long id,
        Long userId,
        String email,
        String firstName,
        String lastName,
        String role,
        Instant joinedAt
) {}
