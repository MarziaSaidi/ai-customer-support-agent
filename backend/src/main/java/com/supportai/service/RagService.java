package com.supportai.service;

import com.supportai.dto.DocumentChunkMatchResponse;
import com.supportai.dto.RagAnswerResponse;
import com.supportai.dto.RagSourceResponse;
import com.supportai.entity.Company;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagService {

    private static final int CONTEXT_LIMIT = 3;
    private static final int EXCERPT_LENGTH = 220;

    private final VectorSearchService vectorSearchService;
    private final OpenAiChatService openAiChatService;

    public RagService(VectorSearchService vectorSearchService, OpenAiChatService openAiChatService) {
        this.vectorSearchService = vectorSearchService;
        this.openAiChatService = openAiChatService;
    }

    public RagAnswerResponse answer(Company company, String question) {
        List<DocumentChunkMatchResponse> matches = vectorSearchService.searchForCompany(
                company.getId(),
                question,
                CONTEXT_LIMIT
        );

        List<RagSourceResponse> sources = matches.stream()
                .map(match -> new RagSourceResponse(
                        match.documentId(),
                        match.documentTitle(),
                        excerpt(match.content()),
                        match.score()
                ))
                .toList();

        if (matches.isEmpty()) {
            return new RagAnswerResponse(
                    "I couldn't find relevant information in the company knowledge base. "
                            + "Would you like me to connect you with a support agent?",
                    List.of()
            );
        }

        String systemPrompt = buildSystemPrompt(company);
        String userPrompt = buildUserPrompt(matches, question);
        String modelAnswer = openAiChatService.chat(systemPrompt, userPrompt);
        String answer = modelAnswer != null ? modelAnswer : fallbackAnswer(matches);

        return new RagAnswerResponse(answer, sources);
    }

    public String formatAnswerWithSources(RagAnswerResponse response) {
        if (response.sources().isEmpty()) {
            return response.answer();
        }

        String titles = response.sources().stream()
                .map(RagSourceResponse::documentTitle)
                .distinct()
                .reduce((left, right) -> left + ", " + right)
                .orElse("");

        return response.answer() + "\n\nSource: " + titles;
    }

    private String buildSystemPrompt(Company company) {
        String basePrompt = company.getAiSystemPrompt() != null && !company.getAiSystemPrompt().isBlank()
                ? company.getAiSystemPrompt()
                : "You are a helpful customer support agent.";

        return basePrompt + """

                Answer using ONLY the documentation context provided by the user.
                If the context does not contain the answer, say you are not sure and offer to escalate to a human agent.
                Be concise, friendly, and accurate.
                Mention the relevant policy or document title naturally in your answer.
                """;
    }

    private String buildUserPrompt(List<DocumentChunkMatchResponse> matches, String question) {
        StringBuilder builder = new StringBuilder("Documentation context:\n\n");
        for (DocumentChunkMatchResponse match : matches) {
            builder.append("[").append(match.documentTitle()).append("]\n");
            builder.append(match.content()).append("\n\n");
        }
        builder.append("Customer question: ").append(question);
        return builder.toString();
    }

    private String fallbackAnswer(List<DocumentChunkMatchResponse> matches) {
        DocumentChunkMatchResponse top = matches.getFirst();
        String summary = excerpt(top.content());
        return "Based on " + top.documentTitle() + ", " + summary;
    }

    private String excerpt(String content) {
        String normalized = content == null ? "" : content.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= EXCERPT_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, EXCERPT_LENGTH).trim() + "...";
    }
}
