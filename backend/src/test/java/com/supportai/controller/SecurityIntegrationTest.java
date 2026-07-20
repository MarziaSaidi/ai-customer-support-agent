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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void protectedEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/tickets").param("companyId", "1"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/analytics").param("companyId", "1"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/documents").param("companyId", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotAccessAnotherCompanyTickets() throws Exception {
        registerCompany("company-a@test.com", "Company A");
        String tokenA = loginToken("company-a@test.com");

        Long companyB = registerCompany("company-b@test.com", "Company B");

        mockMvc.perform(get("/api/tickets")
                        .param("companyId", companyB.toString())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("You do not have access to this company"));
    }

    @Test
    void createTicketRequiresSubject() throws Exception {
        Long companyId = registerCompany("validation@test.com", "Validation Co");
        String token = loginToken("validation@test.com");

        mockMvc.perform(post("/api/tickets")
                        .param("companyId", companyId.toString())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Missing subject"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    private Long registerCompany(String email, String companyName) throws Exception {
        RegisterRequest request = new RegisterRequest(
                email,
                "password123",
                "Test",
                "User",
                companyName
        );

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("companyId").asLong();
    }

    private String loginToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }
}
