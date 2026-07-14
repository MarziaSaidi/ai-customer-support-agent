package com.supportai.controller;

import com.supportai.entity.Company;
import com.supportai.entity.Ticket;
import com.supportai.exception.ResourceNotFoundException;
import com.supportai.repository.CompanyRepository;
import com.supportai.repository.TicketRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/companies")
public class CompanyController {

    private final CompanyRepository companyRepository;

    public CompanyController(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @GetMapping("/{id}")
    public Company getCompany(@PathVariable Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
    }

    @PostMapping("/{id}/settings")
    public Company updateSettings(@PathVariable Long id, @RequestBody Map<String, String> settings) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        if (settings.containsKey("aiSystemPrompt")) {
            company.setAiSystemPrompt(settings.get("aiSystemPrompt"));
        }
        return companyRepository.save(company);
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
    public List<Ticket> listTickets(@RequestParam Long companyId) {
        return ticketRepository.findByCompanyIdOrderByCreatedAtDesc(companyId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Ticket createTicket(@RequestBody Ticket ticket, @RequestParam Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        ticket.setCompany(company);
        return ticketRepository.save(ticket);
    }
}
