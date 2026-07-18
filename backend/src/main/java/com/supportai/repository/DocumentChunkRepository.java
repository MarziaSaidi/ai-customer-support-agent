package com.supportai.repository;

import com.supportai.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {
    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);

    @Modifying
    @Query("DELETE FROM DocumentChunk c WHERE c.document.id = :documentId")
    void deleteByDocumentId(@Param("documentId") Long documentId);

    @Query(value = """
            SELECT c.* FROM document_chunks c
            JOIN documents d ON c.document_id = d.id
            WHERE d.company_id = :companyId AND d.active = true
            ORDER BY c.embedding <=> CAST(:embedding AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<DocumentChunk> findSimilarByCompany(
            @Param("companyId") Long companyId,
            @Param("embedding") String embedding,
            @Param("limit") int limit
    );
}
