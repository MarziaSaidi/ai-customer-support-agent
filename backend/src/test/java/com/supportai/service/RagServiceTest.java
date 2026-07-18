package com.supportai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supportai.dto.RegisterRequest;
import com.supportai.enums.DocumentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RagServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void answersQuestionUsingKnowledgeBaseWithSources() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "rag-admin@test.com",
                "password123",
                "Rag",
                "Admin",
                "Rag Co"
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

        String returnPolicy = """
                Return Policy
                Returns accepted within 60 days if unused with tags attached.
                Refunds are processed within 5-7 business days after receipt.
                """;

        mockMvc.perform(multipart("/api/documents")
                        .file(new MockMultipartFile(
                                "file", "return-policy.txt", "text/plain", returnPolicy.getBytes()))
                        .param("companyId", companyId.toString())
                        .param("title", "Return Policy")
                        .param("type", DocumentType.FAQ.name())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.processed").value(true));

        mockMvc.perform(post("/api/knowledge/ask")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyId": %d,
                                  "question": "Can I return shoes after 30 days?"
                                }
                                """.formatted(companyId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer", containsString("Return Policy")))
                .andExpect(jsonPath("$.sources", hasSize(1)))
                .andExpect(jsonPath("$.sources[0].documentTitle").value("Return Policy"));
    }

    @Test
    void widgetChatUsesKnowledgeBase() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "widget-rag@test.com",
                "password123",
                "Widget",
                "Rag",
                "Widget Co"
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

        mockMvc.perform(multipart("/api/documents")
                        .file(new MockMultipartFile(
                                "file",
                                "shipping.txt",
                                "text/plain",
                                "Standard shipping takes 3-5 business days.".getBytes()))
                        .param("companyId", companyId.toString())
                        .param("title", "Shipping FAQ")
                        .param("type", DocumentType.FAQ.name())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        MvcResult sessionResult = mockMvc.perform(post("/api/chat/widget/sessions")
                        .param("companyId", companyId.toString())
                        .param("customerEmail", "customer@test.com"))
                .andExpect(status().isCreated())
                .andReturn();

        Long sessionId = objectMapper.readTree(sessionResult.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(post("/api/chat/widget/sessions/" + sessionId + "/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"How long does shipping take?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[1].content", containsString("Shipping FAQ")));
    }
}
