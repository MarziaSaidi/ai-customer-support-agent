package com.supportai.controller;

import com.supportai.dto.ChatMessageRequest;
import com.supportai.dto.ChatSessionResponse;
import com.supportai.dto.ConversationSummaryResponse;
import com.supportai.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/widget/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatSessionResponse startWidgetSession(
            @RequestParam Long companyId,
            @RequestParam(required = false) String customerEmail,
            @RequestParam(required = false) String customerName
    ) {
        return chatService.startSession(companyId, customerEmail, customerName);
    }

    @PostMapping("/widget/sessions/{conversationId}/messages")
    public ChatSessionResponse sendWidgetMessage(
            @PathVariable Long conversationId,
            @Valid @RequestBody ChatMessageRequest request
    ) {
        return chatService.sendCustomerMessage(conversationId, request);
    }

    @GetMapping("/sessions")
    public List<ConversationSummaryResponse> getSessions(
            @RequestParam Long companyId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return chatService.getCompanyConversations(companyId, principal.getUsername());
    }

    @GetMapping("/sessions/{conversationId}")
    public ChatSessionResponse getSession(
            @PathVariable Long conversationId,
            @RequestParam Long companyId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return chatService.getConversation(conversationId, companyId, principal.getUsername());
    }

    @PostMapping("/sessions/{conversationId}/messages")
    public ChatSessionResponse sendAgentMessage(
            @PathVariable Long conversationId,
            @Valid @RequestBody ChatMessageRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return chatService.sendAgentMessage(conversationId, request, principal.getUsername());
    }

    @PostMapping("/sessions/{conversationId}/escalate")
    public ChatSessionResponse escalate(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return chatService.escalateToAgent(conversationId, principal.getUsername());
    }

    @PostMapping("/sessions/{conversationId}/resolve")
    public ChatSessionResponse resolve(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return chatService.resolveConversation(conversationId, principal.getUsername());
    }
}
