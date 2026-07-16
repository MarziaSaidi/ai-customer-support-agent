package com.supportai.service;

import com.supportai.dto.AddMemberRequest;
import com.supportai.dto.CompanyMemberResponse;
import com.supportai.dto.CompanyResponse;
import com.supportai.dto.UpdateCompanyRequest;
import com.supportai.entity.Company;
import com.supportai.entity.CompanyUser;
import com.supportai.entity.User;
import com.supportai.enums.RoleType;
import com.supportai.exception.BadRequestException;
import com.supportai.exception.ResourceNotFoundException;
import com.supportai.exception.UnauthorizedException;
import com.supportai.repository.CompanyRepository;
import com.supportai.repository.CompanyUserRepository;
import com.supportai.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyUserRepository companyUserRepository;
    private final UserRepository userRepository;

    public CompanyService(
            CompanyRepository companyRepository,
            CompanyUserRepository companyUserRepository,
            UserRepository userRepository
    ) {
        this.companyRepository = companyRepository;
        this.companyUserRepository = companyUserRepository;
        this.userRepository = userRepository;
    }

    public CompanyResponse getCompany(Long companyId, String requesterEmail) {
        Company company = getCompanyOrThrow(companyId);
        requireMembership(companyId, requesterEmail);
        return toCompanyResponse(company);
    }

    public List<CompanyMemberResponse> listMembers(Long companyId, String requesterEmail) {
        requireMembership(companyId, requesterEmail);
        return companyUserRepository.findByCompanyId(companyId).stream()
                .map(this::toMemberResponse)
                .toList();
    }

    @Transactional
    public CompanyMemberResponse addMember(Long companyId, AddMemberRequest request, String requesterEmail) {
        requireAdmin(companyId, requesterEmail);

        if (request.role() == RoleType.CUSTOMER) {
            throw new BadRequestException("Cannot add customer role to company team");
        }

        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new BadRequestException("User must register before joining a company"));

        if (companyUserRepository.existsByUserIdAndCompanyId(user.getId(), companyId)) {
            throw new BadRequestException("User is already a member of this company");
        }

        Company company = getCompanyOrThrow(companyId);

        CompanyUser membership = new CompanyUser();
        membership.setUser(user);
        membership.setCompany(company);
        membership.setRole(request.role());
        companyUserRepository.save(membership);

        user.setCompany(company);
        user.setRole(request.role());
        userRepository.save(user);

        return toMemberResponse(membership);
    }

    @Transactional
    public void removeMember(Long companyId, Long memberUserId, String requesterEmail) {
        requireAdmin(companyId, requesterEmail);

        User requester = getUserOrThrow(requesterEmail);
        if (requester.getId().equals(memberUserId)) {
            throw new BadRequestException("You cannot remove yourself from the company");
        }

        CompanyUser membership = companyUserRepository.findByUserIdAndCompanyId(memberUserId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if (membership.getRole() == RoleType.ADMIN
                && companyUserRepository.countByCompanyIdAndRole(companyId, RoleType.ADMIN) <= 1) {
            throw new BadRequestException("Cannot remove the only admin");
        }

        companyUserRepository.delete(membership);
    }

    @Transactional
    public CompanyResponse updateCompany(Long companyId, UpdateCompanyRequest request, String requesterEmail) {
        requireAdmin(companyId, requesterEmail);

        Company company = getCompanyOrThrow(companyId);

        if (request.name() != null && !request.name().isBlank()) {
            company.setName(request.name().trim());
        }
        if (request.website() != null) {
            company.setWebsite(request.website().trim());
        }
        if (request.aiSystemPrompt() != null) {
            company.setAiSystemPrompt(request.aiSystemPrompt().trim());
        }

        return toCompanyResponse(companyRepository.save(company));
    }

    @Transactional
    public void createMembership(User user, Company company, RoleType role) {
        CompanyUser membership = new CompanyUser();
        membership.setUser(user);
        membership.setCompany(company);
        membership.setRole(role);
        companyUserRepository.save(membership);
    }

    public RoleType resolveRole(User user) {
        if (user.getCompany() == null) {
            return user.getRole();
        }
        return companyUserRepository.findByUserIdAndCompanyId(user.getId(), user.getCompany().getId())
                .map(CompanyUser::getRole)
                .orElse(user.getRole());
    }

    private void requireMembership(Long companyId, String email) {
        User user = getUserOrThrow(email);
        if (!companyUserRepository.existsByUserIdAndCompanyId(user.getId(), companyId)) {
            throw new UnauthorizedException("You do not have access to this company");
        }
    }

    private void requireAdmin(Long companyId, String email) {
        User user = getUserOrThrow(email);
        CompanyUser membership = companyUserRepository.findByUserIdAndCompanyId(user.getId(), companyId)
                .orElseThrow(() -> new UnauthorizedException("You do not have access to this company"));

        if (membership.getRole() != RoleType.ADMIN) {
            throw new UnauthorizedException("Admin access required");
        }
    }

    private Company getCompanyOrThrow(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
    }

    private User getUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }

    private CompanyResponse toCompanyResponse(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getSlug(),
                company.getWebsite(),
                company.getAiSystemPrompt(),
                company.getCreatedAt()
        );
    }

    private CompanyMemberResponse toMemberResponse(CompanyUser membership) {
        User user = membership.getUser();
        return new CompanyMemberResponse(
                membership.getId(),
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                membership.getRole().name(),
                membership.getCreatedAt()
        );
    }
}
