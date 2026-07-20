package com.supportai.service;

import com.supportai.dto.AnalyticsResponse;
import com.supportai.enums.TicketStatus;
import com.supportai.repository.ConversationRepository;
import com.supportai.repository.FeedbackRepository;
import com.supportai.repository.TicketRepository;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {

    private final ConversationRepository conversationRepository;
    private final TicketRepository ticketRepository;
    private final FeedbackRepository feedbackRepository;

    public AnalyticsService(
            ConversationRepository conversationRepository,
            TicketRepository ticketRepository,
            FeedbackRepository feedbackRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.ticketRepository = ticketRepository;
        this.feedbackRepository = feedbackRepository;
    }

    public AnalyticsResponse getCompanyAnalytics(Long companyId) {
        long totalConversations = conversationRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).size();
        long openTickets = ticketRepository.countByCompanyIdAndStatus(companyId, TicketStatus.OPEN);
        long resolvedTickets = ticketRepository.countByCompanyIdAndStatus(companyId, TicketStatus.RESOLVED);

        double aiResolutionRate = totalConversations == 0 ? 0.0
                : (double) (totalConversations - openTickets) / totalConversations * 100;

        double customerSatisfaction = feedbackRepository.findAll().stream()
                .filter(f -> f.getConversation().getCompany().getId().equals(companyId))
                .mapToInt(f -> f.getRating())
                .average()
                .orElse(0.0);

        return new AnalyticsResponse(
                totalConversations,
                openTickets,
                resolvedTickets,
                aiResolutionRate,
                1200.0,
                customerSatisfaction
        );
    }
}
