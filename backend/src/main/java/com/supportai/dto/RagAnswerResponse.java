package com.supportai.dto;

import java.util.List;

public record RagAnswerResponse(
        String answer,
        List<RagSourceResponse> sources
) {}
