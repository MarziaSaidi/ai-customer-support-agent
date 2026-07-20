package com.supportai.ai;

import java.util.List;
import java.util.Map;

public final class AiToolDefinitions {

    private AiToolDefinitions() {
    }

    public static List<Map<String, Object>> all() {
        return List.of(
                tool("checkOrderStatus",
                        "Look up shipping and delivery status for a customer order",
                        Map.of(
                                "orderNumber", property("string", "Order number, e.g. 48291")
                        ),
                        List.of("orderNumber")),
                tool("createTicket",
                        "Create a support ticket for a human agent when the issue needs escalation",
                        Map.of(
                                "subject", property("string", "Short ticket title"),
                                "description", property("string", "Detailed issue description"),
                                "priority", property("string", "LOW, MEDIUM, HIGH, or URGENT")
                        ),
                        List.of("subject", "description")),
                tool("cancelOrder",
                        "Cancel a customer order if it has not shipped yet",
                        Map.of(
                                "orderNumber", property("string", "Order number to cancel")
                        ),
                        List.of("orderNumber")),
                tool("requestRefund",
                        "Submit a refund request for a delivered or shipped order",
                        Map.of(
                                "orderNumber", property("string", "Order number for the refund")
                        ),
                        List.of("orderNumber")),
                tool("searchDocumentation",
                        "Search company knowledge base documentation to answer policy or product questions",
                        Map.of(
                                "query", property("string", "Question to search in company docs")
                        ),
                        List.of("query"))
        );
    }

    private static Map<String, Object> tool(
            String name,
            String description,
            Map<String, Object> properties,
            List<String> required
    ) {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", name,
                        "description", description,
                        "parameters", Map.of(
                                "type", "object",
                                "properties", properties,
                                "required", required
                        )
                )
        );
    }

    private static Map<String, String> property(String type, String description) {
        return Map.of("type", type, "description", description);
    }
}
