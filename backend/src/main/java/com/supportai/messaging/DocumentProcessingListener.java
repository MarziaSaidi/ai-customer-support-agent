package com.supportai.messaging;

import com.supportai.config.RabbitMqConfig;
import com.supportai.service.DocumentProcessingService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.document-processing.mode", havingValue = "rabbit")
public class DocumentProcessingListener {

    private final DocumentProcessingService documentProcessingService;

    public DocumentProcessingListener(DocumentProcessingService documentProcessingService) {
        this.documentProcessingService = documentProcessingService;
    }

    @RabbitListener(queues = RabbitMqConfig.DOCUMENT_PROCESSING_QUEUE)
    public void handle(DocumentProcessingJob job) {
        documentProcessingService.process(job.documentId());
    }
}
