package com.supportai.service;

import com.supportai.dto.AddTicketNoteRequest;
import com.supportai.dto.UpdateTicketRequest;
import com.supportai.entity.Company;
import com.supportai.entity.CompanyUser;
import com.supportai.entity.Ticket;
import com.supportai.entity.User;
import com.supportai.enums.RoleType;
import com.supportai.enums.TicketStatus;
import com.supportai.repository.CompanyRepository;
import com.supportai.repository.CompanyUserRepository;
import com.supportai.repository.ConversationRepository;
import com.supportai.repository.TicketRepository;
import com.supportai.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceUnitTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private CompanyUserRepository companyUserRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TicketService ticketService;

    @Test
    void updateTicketChangesStatus() {
        stubTeamMember("agent@test.com", 1L, 5L);

        Company company = new Company();
        company.setId(1L);

        Ticket ticket = new Ticket();
        ticket.setId(7L);
        ticket.setCompany(company);
        ticket.setSubject("Help");
        ticket.setStatus(TicketStatus.OPEN);

        when(ticketRepository.findByIdAndCompanyId(7L, 1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        var response = ticketService.updateTicket(
                7L,
                1L,
                new UpdateTicketRequest(TicketStatus.RESOLVED, null, null),
                "agent@test.com"
        );

        assertEquals(TicketStatus.RESOLVED, ticket.getStatus());
        assertEquals(TicketStatus.RESOLVED, response.status());
    }

    @Test
    void addInternalNoteAppendsTimestampedEntry() {
        stubTeamMember("agent@test.com", 1L, 5L);

        Company company = new Company();
        company.setId(1L);

        Ticket ticket = new Ticket();
        ticket.setId(7L);
        ticket.setCompany(company);
        ticket.setSubject("Help");
        ticket.setInternalNotes("Existing note");

        when(ticketRepository.findByIdAndCompanyId(7L, 1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        var response = ticketService.addInternalNote(
                7L,
                1L,
                new AddTicketNoteRequest("Called customer back"),
                "agent@test.com"
        );

        assertTrue(response.internalNotes().contains("Existing note"));
        assertTrue(response.internalNotes().contains("Called customer back"));
        assertTrue(response.internalNotes().contains("Agent User"));
        verify(ticketRepository).save(ticket);
    }

    private void stubTeamMember(String email, Long companyId, Long userId) {
        User user = new User();
        user.setId(userId);
        user.setEmail(email);
        user.setFirstName("Agent");
        user.setLastName("User");

        CompanyUser membership = new CompanyUser();
        membership.setRole(RoleType.SUPPORT_AGENT);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(companyUserRepository.existsByUserIdAndCompanyId(userId, companyId)).thenReturn(true);
        when(companyUserRepository.findByUserIdAndCompanyId(userId, companyId))
                .thenReturn(Optional.of(membership));
    }
}
