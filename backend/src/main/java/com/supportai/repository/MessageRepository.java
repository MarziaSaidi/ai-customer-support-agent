package com.supportai.repository;

import com.supportai.entity.Message;
import com.supportai.enums.MessageRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    @Query("""
            SELECT m FROM Message m
            JOIN FETCH m.conversation c
            WHERE c.company.id = :companyId AND m.role = :role
            ORDER BY m.createdAt DESC
            """)
    List<Message> findByCompanyIdAndRole(
            @Param("companyId") Long companyId,
            @Param("role") MessageRole role
    );
}
