package com.supportai.repository;

import com.supportai.entity.Conversation;
import com.supportai.enums.ConversationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
    List<Conversation> findByCompanyIdAndStatus(Long companyId, ConversationStatus status);
    Optional<Conversation> findByIdAndCompanyId(Long id, Long companyId);
    long countByCompanyIdAndResolvedTrue(Long companyId);
}
