package com.supportai.controller;

import com.supportai.dto.AddMemberRequest;
import com.supportai.dto.CompanyMemberResponse;
import com.supportai.dto.CompanyResponse;
import com.supportai.dto.TicketResponse;
import com.supportai.dto.UpdateCompanyRequest;
import com.supportai.entity.Ticket;
import com.supportai.exception.ResourceNotFoundException;
import com.supportai.repository.CompanyRepository;
import com.supportai.repository.TicketRepository;
import com.supportai.service.CompanyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping("/{id}")
    public CompanyResponse getCompany(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return companyService.getCompany(id, principal.getUsername());
    }

    @PutMapping("/{id}")
    public CompanyResponse updateCompany(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCompanyRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return companyService.updateCompany(id, request, principal.getUsername());
    }

    @GetMapping("/{id}/members")
    public List<CompanyMemberResponse> listMembers(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return companyService.listMembers(id, principal.getUsername());
    }

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyMemberResponse addMember(
            @PathVariable Long id,
            @Valid @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return companyService.addMember(id, request, principal.getUsername());
    }

    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        companyService.removeMember(id, userId, principal.getUsername());
    }
}

@RestController
@RequestMapping("/tickets")
class TicketController {

    private final TicketRepository ticketRepository;
    private final CompanyRepository companyRepository;

    TicketController(TicketRepository ticketRepository, CompanyRepository companyRepository) {
        this.ticketRepository = ticketRepository;
        this.companyRepository = companyRepository;
    }

    @GetMapping
    public List<TicketResponse> listTickets(@RequestParam Long companyId) {
        return ticketRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .map(TicketResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse createTicket(@RequestBody Ticket ticket, @RequestParam Long companyId) {
        var company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        ticket.setCompany(company);
        return TicketResponse.from(ticketRepository.save(ticket));
    }
}
