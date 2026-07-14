package com.supportai.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String EMAIL_QUEUE = "email.notifications";
    public static final String DOCUMENT_PROCESSING_QUEUE = "document.processing";
    public static final String AI_TASKS_QUEUE = "ai.background.tasks";

    @Bean
    public Queue emailQueue() {
        return new Queue(EMAIL_QUEUE, true);
    }

    @Bean
    public Queue documentProcessingQueue() {
        return new Queue(DOCUMENT_PROCESSING_QUEUE, true);
    }

    @Bean
    public Queue aiTasksQueue() {
        return new Queue(AI_TASKS_QUEUE, true);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
