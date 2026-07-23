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
import java.util.List;
import java.util.Map;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    private static final int EMBEDDING_DIMENSION = 1536;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public EmbeddingService(
            ObjectMapper objectMapper,
            @Value("${app.openai.api-key:}") String apiKey,
            @Value("${app.openai.embedding-model:text-embedding-3-small}") String model
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
        factory.setReadTimeout(Duration.ofSeconds(20));
        return factory;
    }

    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            return zeroVector();
        }

        if (apiKey == null || apiKey.isBlank()) {
            return stubEmbedding(text);
        }

        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "input", text
            );

            ResponseEntity<String> response = restClient.post()
                    .uri("/v1/embeddings")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toEntity(String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode embeddingNode = root.path("data").path(0).path("embedding");
            return toFloatArray(embeddingNode);
        } catch (Exception ex) {
            log.warn("OpenAI embedding failed, using stub embedding: {}", ex.getMessage());
            return stubEmbedding(text);
        }
    }

    private float[] toFloatArray(JsonNode embeddingNode) {
        List<Float> values = new ArrayList<>();
        embeddingNode.forEach(node -> values.add((float) node.asDouble()));
        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    private float[] stubEmbedding(String text) {
        float[] vector = new float[EMBEDDING_DIMENSION];
        String[] words = text.toLowerCase().split("\\W+");
        for (String word : words) {
            if (word.isBlank() || word.length() < 3) {
                continue;
            }
            int bucket = Math.floorMod(word.hashCode(), EMBEDDING_DIMENSION);
            vector[bucket] += 1.0f;
        }

        double norm = 0.0;
        for (float value : vector) {
            norm += value * value;
        }
        if (norm == 0.0) {
            return vector;
        }

        float scale = (float) (1.0 / Math.sqrt(norm));
        for (int i = 0; i < vector.length; i++) {
            vector[i] *= scale;
        }
        return vector;
    }

    private float[] zeroVector() {
        return new float[EMBEDDING_DIMENSION];
    }
}
