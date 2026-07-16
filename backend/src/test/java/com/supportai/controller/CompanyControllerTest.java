package com.supportai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supportai.dto.AddMemberRequest;
import com.supportai.dto.RegisterRequest;
import com.supportai.enums.RoleType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void companyMembersAndSettings() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "admin@companytest.com",
                "password123",
                "Admin",
                "User",
                "Test Company"
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

        mockMvc.perform(get("/api/companies/" + companyId + "/members")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].role").value("ADMIN"));

        mockMvc.perform(put("/api/companies/" + companyId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated Company","aiSystemPrompt":"Be helpful."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Company"));

        RegisterRequest agentRequest = new RegisterRequest(
                "agent@companytest.com",
                "password123",
                "Support",
                "Agent",
                "Other Co"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(agentRequest)))
                .andExpect(status().isCreated());

        AddMemberRequest addMember = new AddMemberRequest("agent@companytest.com", RoleType.SUPPORT_AGENT);

        mockMvc.perform(post("/api/companies/" + companyId + "/members")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addMember)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("SUPPORT_AGENT"));

        mockMvc.perform(get("/api/companies/" + companyId + "/members")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void nonAdminCannotUpdateCompany() throws Exception {
        RegisterRequest adminRequest = new RegisterRequest(
                "owner@co.com", "password123", "Owner", "One", "Co One"
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

        RegisterRequest agentRequest = new RegisterRequest(
                "agent@co.com", "password123", "Agent", "Two", "Co Two"
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(agentRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/companies/" + companyId + "/members")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AddMemberRequest("agent@co.com", RoleType.SUPPORT_AGENT))))
                .andExpect(status().isCreated());

        MvcResult agentLogin = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"agent@co.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String agentToken = objectMapper.readTree(agentLogin.getResponse().getContentAsString())
                .get("token").asText();

        mockMvc.perform(put("/api/companies/" + companyId)
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hacked\"}"))
                .andExpect(status().isUnauthorized());
    }
}
