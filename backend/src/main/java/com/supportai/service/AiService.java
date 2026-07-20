package com.supportai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.supportai.ai.AiToolDefinitions;
import com.supportai.dto.RagAnswerResponse;
import com.supportai.entity.Company;
import com.supportai.entity.Conversation;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiService {

    private static final Pattern ORDER_PATTERN = Pattern.compile("#?(\\d{4,})");

    private final AiFunctionService aiFunctionService;
    private final OpenAiChatService openAiChatService;
    private final RagService ragService;

    public AiService(
            AiFunctionService aiFunctionService,
            OpenAiChatService openAiChatService,
            RagService ragService
    ) {
        this.aiFunctionService = aiFunctionService;
        this.openAiChatService = openAiChatService;
        this.ragService = ragService;
    }

    public String generateReply(Conversation conversation, String userMessage) {
        Company company = conversation.getCompany();
        Long companyId = company.getId();
        Long conversationId = conversation.getId();
        String customerEmail = conversation.getCustomerEmail();

        if (openAiChatService.isConfigured()) {
            String toolAnswer = openAiChatService.chatWithTools(
                    buildFunctionCallingSystemPrompt(company),
                    userMessage,
                    AiToolDefinitions.all(),
                    (functionName, arguments) -> executeTool(
                            functionName,
                            arguments,
                            company,
                            companyId,
                            conversationId,
                            customerEmail
                    )
            );
            if (toolAnswer != null && !toolAnswer.isBlank()) {
                return toolAnswer;
            }
        }

        return generateFallbackReply(conversation, userMessage);
    }

    private String executeTool(
            String functionName,
            JsonNode arguments,
            Company company,
            Long companyId,
            Long conversationId,
            String customerEmail
    ) {
        return switch (functionName) {
            case "checkOrderStatus" -> aiFunctionService.checkOrderStatus(
                    textArg(arguments, "orderNumber"),
                    companyId
            );
            case "createTicket" -> aiFunctionService.createTicket(
                    textArg(arguments, "subject"),
                    textArg(arguments, "description"),
                    companyId,
                    conversationId,
                    customerEmail,
                    textArg(arguments, "priority")
            );
            case "cancelOrder" -> aiFunctionService.cancelOrder(
                    textArg(arguments, "orderNumber"),
                    companyId
            );
            case "requestRefund" -> aiFunctionService.requestRefund(
                    textArg(arguments, "orderNumber"),
                    companyId
            );
            case "searchDocumentation" -> aiFunctionService.searchDocumentation(
                    company,
                    textArg(arguments, "query")
            );
            default -> "Unknown function: " + functionName;
        };
    }

    private String generateFallbackReply(Conversation conversation, String userMessage) {
        Company company = conversation.getCompany();
        Long companyId = company.getId();
        String lowerMessage = userMessage.toLowerCase(Locale.ROOT);

        if (containsOrderStatusIntent(lowerMessage)) {
            return aiFunctionService.checkOrderStatus(extractOrderNumber(userMessage), companyId);
        }

        if (lowerMessage.contains("cancel") && ORDER_PATTERN.matcher(userMessage).find()) {
            return aiFunctionService.cancelOrder(extractOrderNumber(userMessage), companyId);
        }

        if (lowerMessage.contains("refund") && ORDER_PATTERN.matcher(userMessage).find()) {
            return aiFunctionService.requestRefund(extractOrderNumber(userMessage), companyId);
        }

        if (containsTicketIntent(lowerMessage)) {
            return aiFunctionService.createTicket(
                    "Customer support request",
                    userMessage,
                    companyId,
                    conversation.getId(),
                    conversation.getCustomerEmail(),
                    "MEDIUM"
            );
        }

        RagAnswerResponse response = ragService.answer(company, userMessage);
        if (response.sources().isEmpty() && containsHelpIntent(lowerMessage)) {
            return aiFunctionService.createTicket(
                    "Escalation from chat",
                    userMessage,
                    companyId,
                    conversation.getId(),
                    conversation.getCustomerEmail(),
                    "HIGH"
            );
        }

        return ragService.formatAnswerWithSources(response);
    }

    private String buildFunctionCallingSystemPrompt(Company company) {
        String basePrompt = company.getAiSystemPrompt() != null && !company.getAiSystemPrompt().isBlank()
                ? company.getAiSystemPrompt()
                : "You are a helpful customer support agent.";

        return basePrompt + """

                You can use tools to check order status, search company documentation, create support tickets,
                cancel orders, or request refunds.
                Use searchDocumentation for policy or FAQ questions.
                Use createTicket when the customer needs a human agent or the issue cannot be resolved automatically.
                Be concise, friendly, and accurate.
                """;
    }

    private boolean containsOrderStatusIntent(String lowerMessage) {
        return lowerMessage.contains("order")
                && (lowerMessage.contains("where") || lowerMessage.contains("status") || lowerMessage.contains("track"));
    }

    private boolean containsTicketIntent(String lowerMessage) {
        return lowerMessage.contains("ticket")
                || lowerMessage.contains("human")
                || lowerMessage.contains("agent")
                || lowerMessage.contains("speak to someone")
                || lowerMessage.contains("talk to someone");
    }

    private boolean containsHelpIntent(String lowerMessage) {
        return lowerMessage.contains("help") || lowerMessage.contains("escalate");
    }

    private String extractOrderNumber(String message) {
        Matcher matcher = ORDER_PATTERN.matcher(message);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String textArg(JsonNode arguments, String field) {
        JsonNode node = arguments.path(field);
        return node.isMissingNode() || node.isNull() ? "" : node.asText();
    }
}
