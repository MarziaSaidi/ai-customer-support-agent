package com.supportai.messaging;

import com.supportai.config.RabbitMqConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.document-processing.mode", havingValue = "rabbit")
public class RabbitDocumentProcessingDispatcher implements DocumentProcessingDispatcher {

    private final RabbitTemplate rabbitTemplate;

    public RabbitDocumentProcessingDispatcher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void dispatch(Long documentId) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.DOCUMENT_PROCESSING_QUEUE,
                new DocumentProcessingJob(documentId)
        );
    }
}
