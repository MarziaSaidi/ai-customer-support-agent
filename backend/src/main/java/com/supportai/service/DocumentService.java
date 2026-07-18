package com.supportai.service;

import com.supportai.dto.DocumentResponse;
import com.supportai.entity.Company;
import com.supportai.entity.Document;
import com.supportai.entity.User;
import com.supportai.enums.DocumentType;
import com.supportai.enums.RoleType;
import com.supportai.exception.BadRequestException;
import com.supportai.exception.ResourceNotFoundException;
import com.supportai.exception.UnauthorizedException;
import com.supportai.repository.CompanyRepository;
import com.supportai.repository.CompanyUserRepository;
import com.supportai.repository.DocumentRepository;
import com.supportai.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.util.List;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final CompanyRepository companyRepository;
    private final CompanyUserRepository companyUserRepository;
    private final UserRepository userRepository;
    private final LocalStorageService localStorageService;

    public DocumentService(
            DocumentRepository documentRepository,
            CompanyRepository companyRepository,
            CompanyUserRepository companyUserRepository,
            UserRepository userRepository,
            LocalStorageService localStorageService
    ) {
        this.documentRepository = documentRepository;
        this.companyRepository = companyRepository;
        this.companyUserRepository = companyUserRepository;
        this.userRepository = userRepository;
        this.localStorageService = localStorageService;
    }

    public List<DocumentResponse> listDocuments(Long companyId, String requesterEmail) {
        requireMembership(companyId, requesterEmail);
        return documentRepository.findByCompanyIdAndActiveTrue(companyId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DocumentResponse uploadDocument(
            Long companyId,
            String title,
            DocumentType type,
            MultipartFile file,
            String requesterEmail
    ) {
        requireAdmin(companyId, requesterEmail);

        if (title == null || title.isBlank()) {
            throw new BadRequestException("Title is required");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        String storedPath = localStorageService.store(companyId, file);
        String filename = file.getOriginalFilename() != null
                ? Paths.get(file.getOriginalFilename()).getFileName().toString()
                : storedPath;

        Document document = new Document();
        document.setCompany(company);
        document.setTitle(title.trim());
        document.setFilename(filename);
        document.setType(type);
        document.setFileUrl(storedPath);
        document.setProcessed(false);
        document.setActive(true);

        return toResponse(documentRepository.save(document));
    }

    @Transactional
    public void deleteDocument(Long documentId, String requesterEmail) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        requireAdmin(document.getCompany().getId(), requesterEmail);
        document.setActive(false);
        documentRepository.save(document);
    }

    private DocumentResponse toResponse(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getFilename(),
                document.getType(),
                document.getFileUrl(),
                document.isProcessed(),
                document.getCreatedAt()
        );
    }

    private void requireMembership(Long companyId, String email) {
        User user = getUserOrThrow(email);
        if (!companyUserRepository.existsByUserIdAndCompanyId(user.getId(), companyId)) {
            throw new UnauthorizedException("You do not have access to this company");
        }
    }

    private void requireAdmin(Long companyId, String email) {
        User user = getUserOrThrow(email);
        var membership = companyUserRepository.findByUserIdAndCompanyId(user.getId(), companyId)
                .orElseThrow(() -> new UnauthorizedException("You do not have access to this company"));

        if (membership.getRole() != RoleType.ADMIN) {
            throw new UnauthorizedException("Admin access required");
        }
    }

    private User getUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }
}
