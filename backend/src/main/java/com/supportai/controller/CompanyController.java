package com.supportai.controller;

import com.supportai.dto.AddMemberRequest;
import com.supportai.dto.CompanyMemberResponse;
import com.supportai.dto.CompanyResponse;
import com.supportai.dto.UpdateCompanyRequest;
import com.supportai.service.CompanyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
    public CompanyMemberResponse addMember(
            @PathVariable Long id,
            @Valid @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return companyService.addMember(id, request, principal.getUsername());
    }

    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void removeMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        companyService.removeMember(id, userId, principal.getUsername());
    }
}
