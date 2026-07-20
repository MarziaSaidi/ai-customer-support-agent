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
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void analyticsDashboardAndTopQuestions() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "analytics-admin@test.com",
                "password123",
                "Analytics",
                "Admin",
                "Analytics Co"
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

        MvcResult sessionResult = mockMvc.perform(post("/api/chat/widget/sessions")
                        .param("companyId", companyId.toString())
                        .param("customerEmail", "customer@test.com"))
                .andExpect(status().isCreated())
                .andReturn();

        Long conversationId = objectMapper.readTree(sessionResult.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(post("/api/chat/widget/sessions/" + conversationId + "/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Where is my order #48291?"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/analytics")
                        .param("companyId", companyId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalConversations").value(1))
                .andExpect(jsonPath("$.conversationTrend", hasSize(7)))
                .andExpect(jsonPath("$.ticketStatusBreakdown.open").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.averageResponseTimeMs").isNumber());

        mockMvc.perform(get("/api/analytics/questions")
                        .param("companyId", companyId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].question", org.hamcrest.Matchers.containsString("order")));
    }
}
