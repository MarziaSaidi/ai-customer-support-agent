package com.supportai.messaging;

import com.supportai.service.DocumentProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@ConditionalOnProperty(name = "app.document-processing.mode", havingValue = "sync", matchIfMissing = true)
public class SyncDocumentProcessingDispatcher implements DocumentProcessingDispatcher {

    private static final Logger log = LoggerFactory.getLogger(SyncDocumentProcessingDispatcher.class);

    private final DocumentProcessingService documentProcessingService;

    public SyncDocumentProcessingDispatcher(DocumentProcessingService documentProcessingService) {
        this.documentProcessingService = documentProcessingService;
    }

    @Async
    @Override
    public void dispatch(Long documentId) {
        try {
            documentProcessingService.process(documentId);
        } catch (RuntimeException ex) {
            log.error("Background document processing failed for id={}: {}", documentId, ex.getMessage());
        }
    }
}
