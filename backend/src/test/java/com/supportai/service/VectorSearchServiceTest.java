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
class VectorSearchServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void findsRelevantChunkForRefundQuestion() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "search-admin@test.com",
                "password123",
                "Search",
                "Admin",
                "Search Co"
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
                Final sale items cannot be returned.
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "return-policy.txt",
                "text/plain",
                returnPolicy.getBytes()
        );

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("companyId", companyId.toString())
                        .param("title", "Return Policy")
                        .param("type", DocumentType.FAQ.name())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.processed").value(true));

        mockMvc.perform(post("/api/documents/search")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyId": %d,
                                  "query": "How do refunds work?",
                                  "limit": 3
                                }
                                """.formatted(companyId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].documentTitle").value("Return Policy"))
                .andExpect(jsonPath("$[0].content", containsString("Refunds")));
    }
}
