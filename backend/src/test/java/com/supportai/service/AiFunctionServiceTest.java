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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiFunctionServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void checkOrderStatusViaWidgetChat() throws Exception {
        Long companyId = registerCompany("order-status@test.com", "Order Status Co");

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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[1].content", containsString("48291")))
                .andExpect(jsonPath("$.messages[1].content", containsString("shipped")));
    }

    @Test
    void searchDocumentationViaWidgetChat() throws Exception {
        Long companyId = registerCompany("docs-search@test.com", "Docs Search Co");
        String token = loginAndGetToken("docs-search@test.com");

        mockMvc.perform(multipart("/api/documents")
                        .file(new MockMultipartFile(
                                "file",
                                "returns.txt",
                                "text/plain",
                                "Returns are accepted within 60 days.".getBytes()))
                        .param("companyId", companyId.toString())
                        .param("title", "Return Policy")
                        .param("type", DocumentType.FAQ.name())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        MvcResult sessionResult = mockMvc.perform(post("/api/chat/widget/sessions")
                        .param("companyId", companyId.toString()))
                .andExpect(status().isCreated())
                .andReturn();

        Long conversationId = objectMapper.readTree(sessionResult.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(post("/api/chat/widget/sessions/" + conversationId + "/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"What is your return policy?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[1].content", containsString("Return Policy")));
    }

    @Test
    void createTicketViaWidgetChat() throws Exception {
        Long companyId = registerCompany("ticket-create@test.com", "Ticket Create Co");
        String token = loginAndGetToken("ticket-create@test.com");

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
                                {"content":"I need to speak to a human agent about a damaged package"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[1].content", containsString("has been created")));

        mockMvc.perform(get("/api/tickets")
                        .param("companyId", companyId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].subject").value("Customer support request"));
    }

    private Long registerCompany(String email, String companyName) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                email,
                "password123",
                "Test",
                "User",
                companyName
        );

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("companyId").asLong();
    }

    private String loginAndGetToken(String email) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("token").asText();
    }
}
