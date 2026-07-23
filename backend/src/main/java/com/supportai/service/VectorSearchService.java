package com.supportai.service;

import com.supportai.dto.DocumentChunkMatchResponse;
import com.supportai.entity.DocumentChunk;
import com.supportai.repository.CompanyUserRepository;
import com.supportai.repository.DocumentChunkRepository;
import com.supportai.repository.UserRepository;
import com.supportai.exception.UnauthorizedException;
import com.supportai.util.CosineSimilarity;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Ranks a company's document chunks against a query using cosine similarity over their
 * embeddings, blended with a small keyword-overlap score. Search is scoped to the caller's
 * company so tenants never see each other's knowledge base.
 */
@Service
public class VectorSearchService {

    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 20;

    private final DocumentChunkRepository documentChunkRepository;
    private final EmbeddingService embeddingService;
    private final CompanyUserRepository companyUserRepository;
    private final UserRepository userRepository;

    public VectorSearchService(
            DocumentChunkRepository documentChunkRepository,
            EmbeddingService embeddingService,
            CompanyUserRepository companyUserRepository,
            UserRepository userRepository
    ) {
        this.documentChunkRepository = documentChunkRepository;
        this.embeddingService = embeddingService;
        this.companyUserRepository = companyUserRepository;
        this.userRepository = userRepository;
    }

    public List<DocumentChunkMatchResponse> search(
            Long companyId,
            String query,
            Integer limit,
            String requesterEmail
    ) {
        requireMembership(companyId, requesterEmail);
        return searchForCompany(companyId, query, limit);
    }

    public List<DocumentChunkMatchResponse> searchForCompany(Long companyId, String query, Integer limit) {
        int resultLimit = limit != null && limit > 0 ? Math.min(limit, MAX_LIMIT) : DEFAULT_LIMIT;
        float[] queryEmbedding = embeddingService.embed(query);

        return documentChunkRepository.findSearchableByCompanyId(companyId).stream()
                .map(chunk -> {
                    double vectorScore = CosineSimilarity.calculate(queryEmbedding, chunk.getEmbedding());
                    double textScore = textRelevanceScore(query, chunk.getContent());
                    double score = (vectorScore * 0.7) + (textScore * 0.3);
                    return toMatch(chunk, score);
                })
                .sorted(Comparator.comparingDouble(DocumentChunkMatchResponse::score).reversed())
                .limit(resultLimit)
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
