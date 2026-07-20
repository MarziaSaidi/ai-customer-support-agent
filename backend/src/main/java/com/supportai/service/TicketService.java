package com.supportai.service;

import com.supportai.dto.AddTicketNoteRequest;
import com.supportai.dto.CreateTicketRequest;
import com.supportai.dto.TicketResponse;
import com.supportai.dto.UpdateTicketRequest;
import com.supportai.entity.Company;
import com.supportai.entity.Conversation;
import com.supportai.entity.Ticket;
import com.supportai.entity.User;
import com.supportai.enums.RoleType;
import com.supportai.enums.TicketPriority;
import com.supportai.enums.TicketStatus;
import com.supportai.exception.BadRequestException;
import com.supportai.exception.ResourceNotFoundException;
import com.supportai.exception.UnauthorizedException;
import com.supportai.repository.CompanyRepository;
import com.supportai.repository.CompanyUserRepository;
import com.supportai.repository.ConversationRepository;
import com.supportai.repository.TicketRepository;
import com.supportai.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class TicketService {

    private static final DateTimeFormatter NOTE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneOffset.UTC);

    private final TicketRepository ticketRepository;
    private final CompanyRepository companyRepository;
    private final ConversationRepository conversationRepository;
    private final CompanyUserRepository companyUserRepository;
    private final UserRepository userRepository;

    public TicketService(
            TicketRepository ticketRepository,
            CompanyRepository companyRepository,
            ConversationRepository conversationRepository,
            CompanyUserRepository companyUserRepository,
            UserRepository userRepository
    ) {
        this.ticketRepository = ticketRepository;
        this.companyRepository = companyRepository;
        this.conversationRepository = conversationRepository;
        this.companyUserRepository = companyUserRepository;
        this.userRepository = userRepository;
    }

    public List<TicketResponse> listTickets(Long companyId, String requesterEmail) {
        requireTeamMember(companyId, requesterEmail);
        return ticketRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .map(TicketResponse::from)
                .toList();
    }

    public TicketResponse getTicket(Long ticketId, Long companyId, String requesterEmail) {
        requireTeamMember(companyId, requesterEmail);
        return TicketResponse.from(getTicketOrThrow(ticketId, companyId));
    }

    @Transactional
    public TicketResponse createTicket(Long companyId, CreateTicketRequest request, String requesterEmail) {
        requireTeamMember(companyId, requesterEmail);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        Ticket ticket = new Ticket();
        ticket.setCompany(company);
        ticket.setSubject(request.subject().trim());
        ticket.setDescription(request.description());
        ticket.setCustomerEmail(request.customerEmail());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(request.priority() != null ? request.priority() : TicketPriority.MEDIUM);

        if (request.conversationId() != null) {
            Conversation conversation = conversationRepository.findByIdAndCompanyId(
                    request.conversationId(),
                    companyId
            ).orElseThrow(() -> new BadRequestException("Conversation not found for this company"));
            ticket.setConversation(conversation);
            if (ticket.getCustomerEmail() == null) {
                ticket.setCustomerEmail(conversation.getCustomerEmail());
            }
        }

        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse updateTicket(
            Long ticketId,
            Long companyId,
            UpdateTicketRequest request,
            String requesterEmail
    ) {
        requireTeamMember(companyId, requesterEmail);
        Ticket ticket = getTicketOrThrow(ticketId, companyId);

        if (request.status() != null) {
            ticket.setStatus(request.status());
        }
        if (request.priority() != null) {
            ticket.setPriority(request.priority());
        }
        if (request.assignedToUserId() != null) {
            if (request.assignedToUserId() == 0) {
                ticket.setAssignedTo(null);
            } else {
                User assignee = userRepository.findById(request.assignedToUserId())
                        .orElseThrow(() -> new BadRequestException("Assignee not found"));
                if (!companyUserRepository.existsByUserIdAndCompanyId(assignee.getId(), companyId)) {
                    throw new BadRequestException("Assignee must be a member of this company");
                }
                ticket.setAssignedTo(assignee);
                if (ticket.getStatus() == TicketStatus.OPEN) {
                    ticket.setStatus(TicketStatus.IN_PROGRESS);
                }
            }
        }

        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse addInternalNote(
            Long ticketId,
            Long companyId,
            AddTicketNoteRequest request,
            String requesterEmail
    ) {
        User author = requireTeamMember(companyId, requesterEmail);
        Ticket ticket = getTicketOrThrow(ticketId, companyId);

        String authorName = author.getFirstName() + " " + author.getLastName();
        ticket.setInternalNotes(appendNote(ticket.getInternalNotes(), authorName, request.note()));

        return TicketResponse.from(ticketRepository.save(ticket));
    }

    private Ticket getTicketOrThrow(Long ticketId, Long companyId) {
        return ticketRepository.findByIdAndCompanyId(ticketId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
    }

    private String appendNote(String existing, String authorName, String note) {
        String entry = "[" + NOTE_TIMESTAMP.format(Instant.now()) + " UTC · " + authorName + "]\n"
                + note.trim();
        if (existing == null || existing.isBlank()) {
            return entry;
        }
        return existing + "\n\n" + entry;
    }

    private User requireTeamMember(Long companyId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!companyUserRepository.existsByUserIdAndCompanyId(user.getId(), companyId)) {
            throw new UnauthorizedException("You do not have access to this company");
        }

        var membership = companyUserRepository.findByUserIdAndCompanyId(user.getId(), companyId)
                .orElseThrow(() -> new UnauthorizedException("You do not have access to this company"));

        if (membership.getRole() == RoleType.CUSTOMER) {
            throw new UnauthorizedException("Team access required");
        }

        return user;
    }
}
