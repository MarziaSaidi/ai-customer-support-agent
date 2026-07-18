package com.supportai.controller;

import com.supportai.dto.KnowledgeAskRequest;
import com.supportai.dto.RagAnswerResponse;
import com.supportai.exception.ResourceNotFoundException;
import com.supportai.repository.CompanyRepository;
import com.supportai.repository.CompanyUserRepository;
import com.supportai.repository.UserRepository;
import com.supportai.exception.UnauthorizedException;
import com.supportai.service.RagService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/knowledge")
public class KnowledgeController {

    private final RagService ragService;
    private final CompanyRepository companyRepository;
    private final CompanyUserRepository companyUserRepository;
    private final UserRepository userRepository;

    public KnowledgeController(
            RagService ragService,
            CompanyRepository companyRepository,
            CompanyUserRepository companyUserRepository,
            UserRepository userRepository
    ) {
        this.ragService = ragService;
        this.companyRepository = companyRepository;
        this.companyUserRepository = companyUserRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/ask")
    public RagAnswerResponse ask(
            @Valid @RequestBody KnowledgeAskRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        requireMembership(request.companyId(), principal.getUsername());

        var company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        return ragService.answer(company, request.question());
    }

    private void requireMembership(Long companyId, String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        if (!companyUserRepository.existsByUserIdAndCompanyId(user.getId(), companyId)) {
            throw new UnauthorizedException("You do not have access to this company");
        }
    }
}
