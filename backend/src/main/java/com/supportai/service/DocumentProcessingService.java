package com.supportai.service;

import com.supportai.entity.Document;
import com.supportai.entity.DocumentChunk;
import com.supportai.exception.BadRequestException;
import com.supportai.exception.ResourceNotFoundException;
import com.supportai.repository.DocumentChunkRepository;
import com.supportai.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DocumentProcessingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingService.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final TextExtractionService textExtractionService;
    private final TextChunkingService textChunkingService;
    private final EmbeddingService embeddingService;

    public DocumentProcessingService(
            DocumentRepository documentRepository,
            DocumentChunkRepository documentChunkRepository,
            TextExtractionService textExtractionService,
            TextChunkingService textChunkingService,
            EmbeddingService embeddingService
    ) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.textExtractionService = textExtractionService;
        this.textChunkingService = textChunkingService;
        this.embeddingService = embeddingService;
    }

    @Transactional
    public void process(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        if (!document.isActive()) {
            return;
        }

        log.info("Processing document id={} title={}", document.getId(), document.getTitle());

        try {
            String extractedText = textExtractionService.extract(document);
            if (extractedText.isBlank()) {
                throw new BadRequestException("No text could be extracted from the document");
            }

            document.setContent(extractedText);
            document.setProcessed(false);
            documentRepository.save(document);

            documentChunkRepository.deleteByDocumentId(documentId);

            List<String> chunks = textChunkingService.chunk(extractedText);
            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunk chunk = new DocumentChunk();
                chunk.setDocument(document);
                chunk.setContent(chunks.get(i));
                chunk.setChunkIndex(i);
                chunk.setEmbedding(embeddingService.embed(chunks.get(i)));
                documentChunkRepository.save(chunk);
            }

            document.setProcessed(true);
            documentRepository.save(document);

            log.info("Processed document id={} with {} chunks", documentId, chunks.size());
        } catch (RuntimeException ex) {
            document.setProcessed(false);
            documentRepository.save(document);
            log.error("Failed to process document id={}: {}", documentId, ex.getMessage());
            throw ex;
        }
    }
}
