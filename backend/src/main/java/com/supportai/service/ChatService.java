package com.supportai.service;

import com.supportai.dto.ChatMessageRequest;
import com.supportai.dto.ChatSessionResponse;
import com.supportai.dto.ConversationSummaryResponse;
import com.supportai.dto.MessageResponse;
import com.supportai.entity.Company;
import com.supportai.entity.Conversation;
import com.supportai.entity.Message;
import com.supportai.entity.User;
import com.supportai.enums.ConversationStatus;
import com.supportai.enums.MessageRole;
import com.supportai.enums.RoleType;
import com.supportai.exception.ResourceNotFoundException;
import com.supportai.exception.UnauthorizedException;
import com.supportai.repository.CompanyRepository;
import com.supportai.repository.CompanyUserRepository;
import com.supportai.repository.ConversationRepository;
import com.supportai.repository.MessageRepository;
import com.supportai.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatService {

    private static final int PREVIEW_LENGTH = 120;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final CompanyRepository companyRepository;
    private final CompanyUserRepository companyUserRepository;
    private final UserRepository userRepository;
    private final AiService aiService;

    public ChatService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            CompanyRepository companyRepository,
            CompanyUserRepository companyUserRepository,
            UserRepository userRepository,
            AiService aiService
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.companyRepository = companyRepository;
        this.companyUserRepository = companyUserRepository;
        this.userRepository = userRepository;
        this.aiService = aiService;
    }

    @Transactional
    public ChatSessionResponse startSession(Long companyId, String customerEmail, String customerName) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        Conversation conversation = new Conversation();
        conversation.setCompany(company);
        conversation.setCustomerEmail(customerEmail);
        conversation.setCustomerName(customerName);
        conversation.setStatus(ConversationStatus.ACTIVE);
        conversationRepository.save(conversation);

        return toSessionResponse(conversation, List.of());
    }

    @Transactional
    public ChatSessionResponse sendCustomerMessage(Long conversationId, ChatMessageRequest request) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        Message customerMessage = new Message();
        customerMessage.setConversation(conversation);
        customerMessage.setRole(MessageRole.CUSTOMER);
        customerMessage.setContent(request.content());
        messageRepository.save(customerMessage);

        if (conversation.getStatus() == ConversationStatus.ACTIVE) {
            String aiReply = aiService.generateReply(conversation, request.content());

            Message aiMessage = new Message();
            aiMessage.setConversation(conversation);
            aiMessage.setRole(MessageRole.AI);
            aiMessage.setContent(aiReply);
            messageRepository.save(aiMessage);
        }

        return getConversationMessages(conversation);
    }

    @Transactional
    public ChatSessionResponse sendAgentMessage(Long conversationId, ChatMessageRequest request, String agentEmail) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        requireTeamMember(conversation.getCompany().getId(), agentEmail);

        Message agentMessage = new Message();
        agentMessage.setConversation(conversation);
        agentMessage.setRole(MessageRole.AGENT);
        agentMessage.setContent(request.content());
        messageRepository.save(agentMessage);

        if (conversation.getStatus() == ConversationStatus.ACTIVE) {
            conversation.setStatus(ConversationStatus.ESCALATED);
        }

        return getConversationMessages(conversation);
    }

    public List<ConversationSummaryResponse> getCompanyConversations(Long companyId, String requesterEmail) {
        requireTeamMember(companyId, requesterEmail);

        return conversationRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    public ChatSessionResponse getConversation(Long conversationId, Long companyId, String requesterEmail) {
        requireTeamMember(companyId, requesterEmail);

        Conversation conversation = conversationRepository.findByIdAndCompanyId(conversationId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        return toSessionResponse(conversation, messages);
    }

    @Transactional
    public ChatSessionResponse escalateToAgent(Long conversationId, String requesterEmail) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        requireTeamMember(conversation.getCompany().getId(), requesterEmail);

        conversation.setStatus(ConversationStatus.ESCALATED);

        Message systemMessage = new Message();
        systemMessage.setConversation(conversation);
        systemMessage.setRole(MessageRole.SYSTEM);
        systemMessage.setContent("Conversation escalated to a human support agent.");
        messageRepository.save(systemMessage);

        return getConversationMessages(conversation);
    }

    @Transactional
    public ChatSessionResponse resolveConversation(Long conversationId, String requesterEmail) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        requireTeamMember(conversation.getCompany().getId(), requesterEmail);

        conversation.setStatus(ConversationStatus.RESOLVED);
        conversation.setResolved(true);

        Message systemMessage = new Message();
        systemMessage.setConversation(conversation);
        systemMessage.setRole(MessageRole.SYSTEM);
        systemMessage.setContent("Conversation marked as resolved.");
        messageRepository.save(systemMessage);

        return getConversationMessages(conversation);
    }

    private ChatSessionResponse getConversationMessages(Conversation conversation) {
        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        return toSessionResponse(conversation, messages);
    }

    private ConversationSummaryResponse toSummaryResponse(Conversation conversation) {
        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        String preview = messages.isEmpty()
                ? ""
                : preview(messages.get(messages.size() - 1).getContent());

        return new ConversationSummaryResponse(
                conversation.getId(),
                conversation.getStatus().name(),
                conversation.getCustomerEmail(),
                conversation.getCustomerName(),
                conversation.isResolved(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                preview,
                messages.size()
        );
    }

    private ChatSessionResponse toSessionResponse(Conversation conversation, List<Message> messages) {
        List<MessageResponse> messageResponses = messages.stream()
                .map(m -> new MessageResponse(m.getId(), m.getRole().name(), m.getContent(), m.getCreatedAt()))
                .toList();

        return new ChatSessionResponse(
                conversation.getId(),
                conversation.getStatus().name(),
                conversation.getCustomerEmail(),
                conversation.getCustomerName(),
                conversation.isResolved(),
                conversation.getCreatedAt(),
                messageResponses
        );
    }

    private String preview(String content) {
        String normalized = content == null ? "" : content.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= PREVIEW_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, PREVIEW_LENGTH).trim() + "...";
    }

    private void requireTeamMember(Long companyId, String email) {
        User user = userRepository.findByEmail(email)
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
