package com.supportai.controller;

import com.supportai.dto.AddTicketNoteRequest;
import com.supportai.dto.CreateTicketRequest;
import com.supportai.dto.TicketResponse;
import com.supportai.dto.UpdateTicketRequest;
import com.supportai.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public List<TicketResponse> listTickets(
            @RequestParam Long companyId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return ticketService.listTickets(companyId, principal.getUsername());
    }

    @GetMapping("/{ticketId}")
    public TicketResponse getTicket(
            @PathVariable Long ticketId,
            @RequestParam Long companyId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return ticketService.getTicket(ticketId, companyId, principal.getUsername());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse createTicket(
            @RequestParam Long companyId,
            @Valid @RequestBody CreateTicketRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return ticketService.createTicket(companyId, request, principal.getUsername());
    }

    @PatchMapping("/{ticketId}")
    public TicketResponse updateTicket(
            @PathVariable Long ticketId,
            @RequestParam Long companyId,
            @RequestBody UpdateTicketRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return ticketService.updateTicket(ticketId, companyId, request, principal.getUsername());
    }

    @PostMapping("/{ticketId}/notes")
    public TicketResponse addInternalNote(
            @PathVariable Long ticketId,
            @RequestParam Long companyId,
            @Valid @RequestBody AddTicketNoteRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return ticketService.addInternalNote(ticketId, companyId, request, principal.getUsername());
    }
}
