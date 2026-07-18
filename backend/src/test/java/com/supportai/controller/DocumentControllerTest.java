package com.supportai.controller;

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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void uploadAndListDocuments() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "docs-admin@test.com",
                "password123",
                "Doc",
                "Admin",
                "Docs Co"
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

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "return-policy.pdf",
                "application/pdf",
                "Sample PDF content for testing".getBytes()
        );

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("companyId", companyId.toString())
                        .param("title", "Return Policy")
                        .param("type", DocumentType.PDF.name())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Return Policy"))
                .andExpect(jsonPath("$.filename").value("return-policy.pdf"))
                .andExpect(jsonPath("$.processed").value(false));

        mockMvc.perform(get("/api/documents")
                        .param("companyId", companyId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void supportAgentCannotUpload() throws Exception {
        RegisterRequest adminRequest = new RegisterRequest(
                "upload-admin@test.com", "password123", "Up", "Admin", "Upload Co"
        );
        MvcResult adminResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long companyId = objectMapper.readTree(adminResult.getResponse().getContentAsString())
                .get("companyId").asLong();
        String adminToken = objectMapper.readTree(adminResult.getResponse().getContentAsString())
                .get("token").asText();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                "upload-agent@test.com", "password123", "Up", "Agent", "Other Co"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/companies/" + companyId + "/members")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"upload-agent@test.com","role":"SUPPORT_AGENT"}
                                """))
                .andExpect(status().isCreated());

        MvcResult agentLogin = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"upload-agent@test.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String agentToken = objectMapper.readTree(agentLogin.getResponse().getContentAsString())
                .get("token").asText();

        MockMultipartFile file = new MockMultipartFile(
                "file", "faq.pdf", "application/pdf", "content".getBytes()
        );

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("companyId", companyId.toString())
                        .param("title", "FAQ")
                        .param("type", DocumentType.PDF.name())
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isUnauthorized());
    }
}
