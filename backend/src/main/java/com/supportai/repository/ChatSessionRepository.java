package com.supportai.repository;

import com.supportai.entity.ChatSession;
import com.supportai.enums.ChatSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    List<ChatSession> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
    List<ChatSession> findByCompanyIdAndStatus(Long companyId, ChatSessionStatus status);
}
