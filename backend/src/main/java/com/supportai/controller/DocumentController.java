package com.supportai.controller;

import com.supportai.dto.DocumentResponse;
import com.supportai.enums.DocumentType;
import com.supportai.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public List<DocumentResponse> listDocuments(
            @RequestParam Long companyId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return documentService.listDocuments(companyId, principal.getUsername());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse uploadDocument(
            @RequestParam Long companyId,
            @RequestParam String title,
            @RequestParam DocumentType type,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return documentService.uploadDocument(companyId, title, type, file, principal.getUsername());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal
    ) {
        documentService.deleteDocument(id, principal.getUsername());
    }

    @PostMapping("/{id}/reprocess")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void reprocessDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal
    ) {
        documentService.reprocessDocument(id, principal.getUsername());
    }
}
