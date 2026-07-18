package com.supportai.service;

import com.supportai.entity.Document;
import com.supportai.enums.DocumentType;
import com.supportai.exception.BadRequestException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class TextExtractionService {

    private final LocalStorageService localStorageService;

    public TextExtractionService(LocalStorageService localStorageService) {
        this.localStorageService = localStorageService;
    }

    public String extract(Document document) {
        Path filePath = localStorageService.resolveStoredPath(document.getFileUrl());
        DocumentType type = document.getType();

        return switch (type) {
            case PDF -> extractPdf(filePath);
            case MARKDOWN, FAQ -> extractPlainText(filePath);
            case WORD -> throw new BadRequestException("Word document processing is not supported yet. Use PDF or Markdown.");
            case WEBSITE -> throw new BadRequestException("Website import is not supported yet.");
        };
    }

    private String extractPdf(Path filePath) {
        try (PDDocument pdf = Loader.loadPDF(filePath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pdf);
            return normalize(text);
        } catch (IOException ex) {
            throw new BadRequestException("Failed to extract text from PDF");
        }
    }

    private String extractPlainText(Path filePath) {
        try {
            return normalize(Files.readString(filePath, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new BadRequestException("Failed to read document file");
        }
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n").trim();
    }
}
