package com.supportai.controller;

import com.supportai.dto.ChatMessageRequest;
import com.supportai.dto.ChatSessionResponse;
import com.supportai.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    @PostMapping("/widget/sessions/{sessionId}/messages")
    public ChatSessionResponse sendWidgetMessage(
            @PathVariable Long sessionId,
            @Valid @RequestBody ChatMessageRequest request
    ) {
        return chatService.sendMessage(sessionId, request);
    }

    @GetMapping("/sessions")
    public List<ChatSessionResponse> getSessions(@RequestParam Long companyId) {
        return chatService.getCompanySessions(companyId);
    }

    @PostMapping("/sessions/{sessionId}/escalate")
    public ChatSessionResponse escalate(@PathVariable Long sessionId) {
        return chatService.escalateToAgent(sessionId);
    }
}
