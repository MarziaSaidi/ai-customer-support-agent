package com.supportai.service;

import com.supportai.dto.ChatMessageRequest;
import com.supportai.dto.ChatSessionResponse;
import com.supportai.dto.MessageResponse;
import com.supportai.entity.ChatSession;
import com.supportai.entity.Company;
import com.supportai.entity.Message;
import com.supportai.enums.ChatSessionStatus;
import com.supportai.enums.MessageRole;
import com.supportai.exception.ResourceNotFoundException;
import com.supportai.repository.ChatSessionRepository;
import com.supportai.repository.CompanyRepository;
import com.supportai.repository.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final MessageRepository messageRepository;
    private final CompanyRepository companyRepository;
    private final AiService aiService;

    public ChatService(
            ChatSessionRepository chatSessionRepository,
            MessageRepository messageRepository,
            CompanyRepository companyRepository,
            AiService aiService
    ) {
        this.chatSessionRepository = chatSessionRepository;
        this.messageRepository = messageRepository;
        this.companyRepository = companyRepository;
        this.aiService = aiService;
    }

    @Transactional
    public ChatSessionResponse startSession(Long companyId, String customerEmail, String customerName) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        ChatSession session = new ChatSession();
        session.setCompany(company);
        session.setCustomerEmail(customerEmail);
        session.setCustomerName(customerName);
        session.setStatus(ChatSessionStatus.ACTIVE);
        chatSessionRepository.save(session);

        return toSessionResponse(session, List.of());
    }

    @Transactional
    public ChatSessionResponse sendMessage(Long sessionId, ChatMessageRequest request) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found"));

        Message customerMessage = new Message();
        customerMessage.setSession(session);
        customerMessage.setRole(MessageRole.CUSTOMER);
        customerMessage.setContent(request.content());
        messageRepository.save(customerMessage);

        String aiReply = aiService.generateReply(session, request.content());

        Message aiMessage = new Message();
        aiMessage.setSession(session);
        aiMessage.setRole(MessageRole.AI);
        aiMessage.setContent(aiReply);
        messageRepository.save(aiMessage);

        List<Message> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        return toSessionResponse(session, messages);
    }

    public List<ChatSessionResponse> getCompanySessions(Long companyId) {
        return chatSessionRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .map(session -> {
                    List<Message> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
                    return toSessionResponse(session, messages);
                })
                .toList();
    }

    @Transactional
    public ChatSessionResponse escalateToAgent(Long sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found"));
        session.setStatus(ChatSessionStatus.ESCALATED);

        Message systemMessage = new Message();
        systemMessage.setSession(session);
        systemMessage.setRole(MessageRole.SYSTEM);
        systemMessage.setContent("Conversation escalated to a human support agent.");
        messageRepository.save(systemMessage);

        List<Message> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        return toSessionResponse(session, messages);
    }

    private ChatSessionResponse toSessionResponse(ChatSession session, List<Message> messages) {
        List<MessageResponse> messageResponses = messages.stream()
                .map(m -> new MessageResponse(m.getId(), m.getRole().name(), m.getContent(), m.getCreatedAt()))
                .toList();

        return new ChatSessionResponse(
                session.getId(),
                session.getStatus().name(),
                session.getCustomerEmail(),
                session.getCustomerName(),
                session.isResolved(),
                session.getCreatedAt(),
                messageResponses
        );
    }
}
