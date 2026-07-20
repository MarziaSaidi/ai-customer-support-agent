package com.supportai.dto;

import jakarta.validation.constraints.NotBlank;

public record AddTicketNoteRequest(
        @NotBlank String note
) {}
