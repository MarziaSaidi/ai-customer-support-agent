package com.supportai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supportai.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void widgetChatAndAgentInbox() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "chat-admin@test.com",
                "password123",
                "Chat",
                "Admin",
                "Chat Test Co"
        );

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String token = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("token").asText();
        Long companyId = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("companyId").asLong();

        MvcResult widgetSessionResult = mockMvc.perform(post("/api/chat/widget/sessions")
                        .param("companyId", companyId.toString())
                        .param("customerEmail", "customer@test.com")
                        .param("customerName", "Test Customer"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        Long conversationId = objectMapper.readTree(widgetSessionResult.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(post("/api/chat/widget/sessions/" + conversationId + "/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Where is my order #48291?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages", hasSize(greaterThanOrEqualTo(2))));

        mockMvc.perform(get("/api/chat/sessions")
                        .param("companyId", companyId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].messageCount").value(2));

        mockMvc.perform(get("/api/chat/sessions/" + conversationId)
                        .param("companyId", companyId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages", hasSize(2)));

        mockMvc.perform(post("/api/chat/sessions/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"An agent is reviewing your order now."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ESCALATED"))
                .andExpect(jsonPath("$.messages", hasSize(3)));

        mockMvc.perform(post("/api/chat/sessions/" + conversationId + "/resolve")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolved").value(true))
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }
}
