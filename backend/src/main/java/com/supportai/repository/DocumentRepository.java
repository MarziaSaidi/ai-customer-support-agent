package com.supportai.repository;

import com.supportai.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByCompanyIdAndActiveTrue(Long companyId);
}
