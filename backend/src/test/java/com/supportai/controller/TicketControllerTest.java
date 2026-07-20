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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createUpdateAssignAndAddNotes() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "ticket-admin@test.com",
                "password123",
                "Ticket",
                "Admin",
                "Ticket Co"
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

        MvcResult meResult = mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        Long adminUserId = objectMapper.readTree(meResult.getResponse().getContentAsString())
                .get("id").asLong();

        MvcResult createResult = mockMvc.perform(post("/api/tickets")
                        .param("companyId", companyId.toString())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subject": "Damaged package",
                                  "description": "Customer received a torn box",
                                  "priority": "HIGH",
                                  "customerEmail": "buyer@test.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn();

        Long ticketId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(get("/api/tickets/" + ticketId)
                        .param("companyId", companyId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("Damaged package"));

        mockMvc.perform(get("/api/tickets")
                        .param("companyId", companyId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(patch("/api/tickets/" + ticketId)
                        .param("companyId", companyId.toString())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assignedToUserId": %d}
                                """.formatted(adminUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.assignedToUserId").value(adminUserId.intValue()));

        mockMvc.perform(patch("/api/tickets/" + ticketId)
                        .param("companyId", companyId.toString())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"RESOLVED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        mockMvc.perform(post("/api/tickets/" + ticketId + "/notes")
                        .param("companyId", companyId.toString())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"note":"Replacement shipped via expedited delivery"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.internalNotes", containsString("Replacement shipped via expedited delivery")));
    }
}
