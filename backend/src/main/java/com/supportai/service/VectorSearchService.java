package com.supportai.service;

import com.supportai.dto.DocumentChunkMatchResponse;
import com.supportai.entity.DocumentChunk;
import com.supportai.repository.CompanyUserRepository;
import com.supportai.repository.DocumentChunkRepository;
import com.supportai.repository.UserRepository;
import com.supportai.exception.UnauthorizedException;
import com.supportai.util.CosineSimilarity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class VectorSearchService {

    private static final int DEFAULT_LIMIT = 5;

    private final DocumentChunkRepository documentChunkRepository;
    private final EmbeddingService embeddingService;
    private final CompanyUserRepository companyUserRepository;
    private final UserRepository userRepository;
    private final String mode;

    public VectorSearchService(
            DocumentChunkRepository documentChunkRepository,
            EmbeddingService embeddingService,
            CompanyUserRepository companyUserRepository,
            UserRepository userRepository,
            @Value("${app.vector-search.mode:in-memory}") String mode
    ) {
        this.documentChunkRepository = documentChunkRepository;
        this.embeddingService = embeddingService;
        this.companyUserRepository = companyUserRepository;
        this.userRepository = userRepository;
        this.mode = mode;
    }

    public List<DocumentChunkMatchResponse> search(
            Long companyId,
            String query,
            Integer limit,
            String requesterEmail
    ) {
        requireMembership(companyId, requesterEmail);

        int resultLimit = limit != null && limit > 0 ? Math.min(limit, 20) : DEFAULT_LIMIT;
        float[] queryEmbedding = embeddingService.embed(query);

        List<DocumentChunkMatchResponse> results = "pgvector".equalsIgnoreCase(mode)
                ? searchWithPgVector(companyId, queryEmbedding, resultLimit)
                : searchInMemory(companyId, query, queryEmbedding, resultLimit);

        return results.stream()
                .sorted(Comparator.comparingDouble(DocumentChunkMatchResponse::score).reversed())
                .limit(resultLimit)
                .toList();
    }

    private List<DocumentChunkMatchResponse> searchWithPgVector(
            Long companyId,
            float[] queryEmbedding,
            int limit
    ) {
        String embeddingLiteral = embeddingService.toPgVectorLiteral(queryEmbedding);
        List<Long> chunkIds = documentChunkRepository.findSimilarChunkIdsByCompany(
                companyId,
                embeddingLiteral,
                limit
        );

        if (chunkIds.isEmpty()) {
            return List.of();
        }

        return documentChunkRepository.findByIdsWithDocument(chunkIds).stream()
                .map(chunk -> toMatch(chunk, 1.0))
                .toList();
    }

    private List<DocumentChunkMatchResponse> searchInMemory(
            Long companyId,
            String query,
            float[] queryEmbedding,
            int limit
    ) {
        return documentChunkRepository.findSearchableByCompanyId(companyId).stream()
                .map(chunk -> {
                    double vectorScore = CosineSimilarity.calculate(queryEmbedding, chunk.getEmbedding());
                    double textScore = textRelevanceScore(query, chunk.getContent());
                    double score = (vectorScore * 0.7) + (textScore * 0.3);
                    return toMatch(chunk, score);
                })
                .sorted(Comparator.comparingDouble(DocumentChunkMatchResponse::score).reversed())
                .limit(limit)
                .toList();
    }

    private double textRelevanceScore(String query, String content) {
        String[] queryTerms = query.toLowerCase(Locale.ROOT).split("\\W+");
        String haystack = content.toLowerCase(Locale.ROOT);
        if (queryTerms.length == 0) {
            return 0.0;
        }

        int matches = 0;
        for (String term : queryTerms) {
            if (term.length() > 2 && haystack.contains(term)) {
                matches++;
            }
        }
        return (double) matches / queryTerms.length;
    }

    private DocumentChunkMatchResponse toMatch(DocumentChunk chunk, double score) {
        return new DocumentChunkMatchResponse(
                chunk.getId(),
                chunk.getDocument().getId(),
                chunk.getDocument().getTitle(),
                chunk.getContent(),
                chunk.getChunkIndex(),
                score
        );
    }

    private void requireMembership(Long companyId, String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        if (!companyUserRepository.existsByUserIdAndCompanyId(user.getId(), companyId)) {
            throw new UnauthorizedException("You do not have access to this company");
        }
    }
}
