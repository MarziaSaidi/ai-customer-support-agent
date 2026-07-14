package com.supportai.repository;

import com.supportai.entity.Ticket;
import com.supportai.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
    List<Ticket> findByCompanyIdAndStatus(Long companyId, TicketStatus status);
    long countByCompanyIdAndStatus(Long companyId, TicketStatus status);
}
