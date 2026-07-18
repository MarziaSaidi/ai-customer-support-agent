package com.supportai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supportai.dto.RegisterRequest;
import com.supportai.enums.DocumentType;
import com.supportai.repository.DocumentChunkRepository;
import com.supportai.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentProcessingServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @Test
    void uploadTriggersProcessingPipeline() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "pipeline-admin@test.com",
                "password123",
                "Pipe",
                "Line",
                "Pipeline Co"
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

        String faqText = """
                SHIPPING FAQ
                Standard shipping takes 3-5 business days.
                Express shipping takes 1-2 business days.
                Free shipping on orders over $75.
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "shipping-faq.txt",
                "text/plain",
                faqText.getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("companyId", companyId.toString())
                        .param("title", "Shipping FAQ")
                        .param("type", DocumentType.FAQ.name())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.processed").value(true))
                .andReturn();

        Long documentId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .get("id")
                .asLong();

        var document = documentRepository.findById(documentId).orElseThrow();
        assertThat(document.isProcessed()).isTrue();
        assertThat(document.getContent()).contains("Standard shipping");

        var chunks = documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.getFirst().getContent()).isNotBlank();
        assertThat(chunks.getFirst().getEmbedding()).isNotNull();
    }
}
