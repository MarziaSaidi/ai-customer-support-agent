package com.supportai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@Service
public class OpenAiChatService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiChatService.class);
    private static final int MAX_TOOL_ROUNDS = 5;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public OpenAiChatService(
            ObjectMapper objectMapper,
            @Value("${app.openai.api-key:}") String apiKey,
            @Value("${app.openai.model:gpt-4o-mini}") String model
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com")
                .requestFactory(timeoutRequestFactory())
                .build();
    }

    private static SimpleClientHttpRequestFactory timeoutRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(30));
        return factory;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String chat(String systemPrompt, String userPrompt) {
        if (!isConfigured()) {
            return null;
        }

        List<Map<String, Object>> messages = List.of(
                message("system", systemPrompt, null),
                message("user", userPrompt, null)
        );

        return complete(messages, null);
    }

    public String chatWithTools(
            String systemPrompt,
            String userMessage,
            List<Map<String, Object>> tools,
            BiFunction<String, JsonNode, String> toolHandler
    ) {
        if (!isConfigured()) {
            return null;
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(message("system", systemPrompt, null));
        messages.add(message("user", userMessage, null));

        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            JsonNode assistantMessage = requestCompletion(messages, tools);
            if (assistantMessage == null) {
                return null;
            }

            JsonNode toolCalls = assistantMessage.path("tool_calls");
            if (toolCalls.isMissingNode() || !toolCalls.isArray() || toolCalls.isEmpty()) {
                return assistantMessage.path("content").asText(null);
            }

            messages.add(assistantToolMessage(assistantMessage));

            for (JsonNode toolCall : toolCalls) {
                String toolCallId = toolCall.path("id").asText();
                String functionName = toolCall.path("function").path("name").asText();
                JsonNode arguments = parseArguments(toolCall.path("function").path("arguments").asText("{}"));
                String result = toolHandler.apply(functionName, arguments);
                messages.add(toolResultMessage(toolCallId, result));
            }
        }

        return null;
    }

    private String complete(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        JsonNode assistantMessage = requestCompletion(messages, tools);
        return assistantMessage == null ? null : assistantMessage.path("content").asText(null);
    }

    private JsonNode requestCompletion(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("messages", messages);
            body.put("temperature", 0.2);
            if (tools != null && !tools.isEmpty()) {
                body.put("tools", tools);
                body.put("tool_choice", "auto");
            }

            ResponseEntity<String> response = restClient.post()
                    .uri("/v1/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toEntity(String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").path(0).path("message");
        } catch (Exception ex) {
            log.warn("OpenAI chat completion failed: {}", ex.getMessage());
            return null;
        }
    }

    private Map<String, Object> message(String role, String content, JsonNode toolCalls) {
        Map<String, Object> message = new HashMap<>();
        message.put("role", role);
        if (content != null) {
            message.put("content", content);
        }
        if (toolCalls != null) {
            message.put("tool_calls", objectMapper.convertValue(toolCalls, List.class));
        }
        return message;
    }

    private Map<String, Object> assistantToolMessage(JsonNode assistantMessage) {
        Map<String, Object> message = new HashMap<>();
        message.put("role", "assistant");
        if (assistantMessage.hasNonNull("content")) {
            message.put("content", assistantMessage.path("content").asText());
        } else {
            message.put("content", null);
        }
        message.put("tool_calls", objectMapper.convertValue(assistantMessage.path("tool_calls"), List.class));
        return message;
    }

    private Map<String, Object> toolResultMessage(String toolCallId, String content) {
        Map<String, Object> message = new HashMap<>();
        message.put("role", "tool");
        message.put("tool_call_id", toolCallId);
        message.put("content", content);
        return message;
    }

    private JsonNode parseArguments(String rawArguments) {
        try {
            return objectMapper.readTree(rawArguments);
        } catch (Exception ex) {
            return objectMapper.createObjectNode();
        }
    }
}
