package com.supportai.dto;

public record UserProfileResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String role,
        Long companyId,
        String companyName,
        boolean emailVerified
) {}
