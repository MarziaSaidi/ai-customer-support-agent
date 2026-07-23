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

    @Query("""
            SELECT c FROM DocumentChunk c
            JOIN FETCH c.document d
            WHERE d.company.id = :companyId AND d.active = true AND d.processed = true
            """)
    List<DocumentChunk> findSearchableByCompanyId(@Param("companyId") Long companyId);
}
