package com.supportai.repository;

import com.supportai.entity.AIEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AIEmbeddingRepository extends JpaRepository<AIEmbedding, Long> {
    List<AIEmbedding> findByDocumentId(Long documentId);

    @Query(value = """
            SELECT e.* FROM ai_embeddings e
            JOIN documents d ON e.document_id = d.id
            WHERE d.company_id = :companyId AND d.active = true
            ORDER BY e.embedding <=> CAST(:embedding AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<AIEmbedding> findSimilarByCompany(
            @Param("companyId") Long companyId,
            @Param("embedding") String embedding,
            @Param("limit") int limit
    );
}
