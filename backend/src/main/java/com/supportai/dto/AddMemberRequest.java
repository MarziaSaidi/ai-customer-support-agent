package com.supportai.dto;

import com.supportai.enums.RoleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddMemberRequest(
        @NotBlank @Email String email,
        @NotNull RoleType role
) {}
