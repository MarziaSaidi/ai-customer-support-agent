package com.supportai.repository;

import com.supportai.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByConversationId(Long conversationId);

    @Query("SELECT f FROM Feedback f JOIN f.conversation c WHERE c.company.id = :companyId")
    List<Feedback> findByCompanyId(@Param("companyId") Long companyId);
}
