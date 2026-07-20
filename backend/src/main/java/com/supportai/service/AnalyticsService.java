package com.supportai.service;

import com.supportai.dto.AnalyticsResponse;
import com.supportai.dto.QuestionStatResponse;
import com.supportai.dto.TicketStatusBreakdown;
import com.supportai.dto.TrendPointResponse;
import com.supportai.entity.Conversation;
import com.supportai.entity.Message;
import com.supportai.enums.MessageRole;
import com.supportai.enums.RoleType;
import com.supportai.enums.TicketStatus;
import com.supportai.exception.UnauthorizedException;
import com.supportai.repository.CompanyUserRepository;
import com.supportai.repository.ConversationRepository;
import com.supportai.repository.FeedbackRepository;
import com.supportai.repository.MessageRepository;
import com.supportai.repository.TicketRepository;
import com.supportai.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private static final int TREND_DAYS = 7;
    private static final int TOP_QUESTION_LIMIT = 5;
    private static final int QUESTION_PREVIEW_LENGTH = 100;

    private final ConversationRepository conversationRepository;
    private final TicketRepository ticketRepository;
    private final FeedbackRepository feedbackRepository;
    private final MessageRepository messageRepository;
    private final CompanyUserRepository companyUserRepository;
    private final UserRepository userRepository;

    public AnalyticsService(
            ConversationRepository conversationRepository,
            TicketRepository ticketRepository,
            FeedbackRepository feedbackRepository,
            MessageRepository messageRepository,
            CompanyUserRepository companyUserRepository,
            UserRepository userRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.ticketRepository = ticketRepository;
        this.feedbackRepository = feedbackRepository;
        this.messageRepository = messageRepository;
        this.companyUserRepository = companyUserRepository;
        this.userRepository = userRepository;
    }

    public AnalyticsResponse getCompanyAnalytics(Long companyId, String requesterEmail) {
        requireTeamMember(companyId, requesterEmail);

        List<Conversation> conversations = conversationRepository.findByCompanyIdOrderByCreatedAtDesc(companyId);
        long totalConversations = conversations.size();
        long resolvedConversations = conversationRepository.countByCompanyIdAndResolvedTrue(companyId);

        long openTickets = ticketRepository.countByCompanyIdAndStatus(companyId, TicketStatus.OPEN);
        long inProgressTickets = ticketRepository.countByCompanyIdAndStatus(companyId, TicketStatus.IN_PROGRESS);
        long resolvedTickets = ticketRepository.countByCompanyIdAndStatus(companyId, TicketStatus.RESOLVED);
        long closedTickets = ticketRepository.countByCompanyIdAndStatus(companyId, TicketStatus.CLOSED);

        double aiResolutionRate = totalConversations == 0
                ? 0.0
                : (double) resolvedConversations / totalConversations * 100;

        double customerSatisfaction = feedbackRepository.findByCompanyId(companyId).stream()
                .mapToInt(f -> f.getRating())
                .average()
                .orElse(0.0);

        return new AnalyticsResponse(
                totalConversations,
                resolvedConversations,
                openTickets,
                resolvedTickets,
                aiResolutionRate,
                calculateAverageResponseTimeMs(conversations),
                customerSatisfaction,
                buildConversationTrend(conversations),
                getTopQuestions(companyId),
                new TicketStatusBreakdown(openTickets, inProgressTickets, resolvedTickets, closedTickets)
        );
    }

    public List<QuestionStatResponse> getTopQuestions(Long companyId, String requesterEmail) {
        requireTeamMember(companyId, requesterEmail);
        return getTopQuestions(companyId);
    }

    private List<QuestionStatResponse> getTopQuestions(Long companyId) {
        List<Message> customerMessages = messageRepository.findByCompanyIdAndRole(companyId, MessageRole.CUSTOMER);

        Map<String, Long> counts = new LinkedHashMap<>();
        Map<String, String> labels = new LinkedHashMap<>();

        for (Message message : customerMessages) {
            String normalized = normalizeQuestion(message.getContent());
            if (normalized.length() < 8) {
                continue;
            }
            counts.merge(normalized, 1L, Long::sum);
            labels.putIfAbsent(normalized, previewQuestion(message.getContent()));
        }

        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(TOP_QUESTION_LIMIT)
                .map(entry -> new QuestionStatResponse(labels.get(entry.getKey()), entry.getValue()))
                .collect(Collectors.toList());
    }

    private double calculateAverageResponseTimeMs(List<Conversation> conversations) {
        List<Long> responseTimes = new ArrayList<>();

        for (Conversation conversation : conversations) {
            List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
            for (int i = 0; i < messages.size() - 1; i++) {
                Message current = messages.get(i);
                Message next = messages.get(i + 1);
                if (current.getRole() == MessageRole.CUSTOMER && next.getRole() == MessageRole.AI) {
                    Instant customerAt = current.getCreatedAt();
                    Instant aiAt = next.getCreatedAt();
                    if (customerAt != null && aiAt != null) {
                        responseTimes.add(Duration.between(customerAt, aiAt).toMillis());
                    }
                }
            }
        }

        return responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    private List<TrendPointResponse> buildConversationTrend(List<Conversation> conversations) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Map<LocalDate, Long> countsByDay = new LinkedHashMap<>();
        for (int i = TREND_DAYS - 1; i >= 0; i--) {
            countsByDay.put(today.minusDays(i), 0L);
        }

        for (Conversation conversation : conversations) {
            if (conversation.getCreatedAt() == null) {
                continue;
            }
            LocalDate day = conversation.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
            if (countsByDay.containsKey(day)) {
                countsByDay.merge(day, 1L, Long::sum);
            }
        }

        return countsByDay.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> new TrendPointResponse(entry.getKey().toString(), entry.getValue()))
                .toList();
    }

    private String normalizeQuestion(String content) {
        if (content == null) {
            return "";
        }
        return content.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private String previewQuestion(String content) {
        String normalized = content == null ? "" : content.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= QUESTION_PREVIEW_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, QUESTION_PREVIEW_LENGTH).trim() + "...";
    }

    private void requireTeamMember(Long companyId, String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!companyUserRepository.existsByUserIdAndCompanyId(user.getId(), companyId)) {
            throw new UnauthorizedException("You do not have access to this company");
        }

        var membership = companyUserRepository.findByUserIdAndCompanyId(user.getId(), companyId)
                .orElseThrow(() -> new UnauthorizedException("You do not have access to this company"));

        if (membership.getRole() == RoleType.CUSTOMER) {
            throw new UnauthorizedException("Team access required");
        }
    }
}
