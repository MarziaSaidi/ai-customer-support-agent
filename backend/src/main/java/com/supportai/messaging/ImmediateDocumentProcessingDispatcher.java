package com.supportai.messaging;

import com.supportai.service.DocumentProcessingService;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Primary
@Profile("test")
public class ImmediateDocumentProcessingDispatcher implements DocumentProcessingDispatcher {

    private final DocumentProcessingService documentProcessingService;

    public ImmediateDocumentProcessingDispatcher(DocumentProcessingService documentProcessingService) {
        this.documentProcessingService = documentProcessingService;
    }

    @Override
    public void dispatch(Long documentId) {
        documentProcessingService.process(documentId);
    }
}
