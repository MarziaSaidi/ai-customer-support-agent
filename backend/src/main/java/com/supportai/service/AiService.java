package com.supportai.service;

import com.supportai.dto.RagAnswerResponse;
import com.supportai.entity.Conversation;
import com.supportai.entity.Order;
import com.supportai.entity.Refund;
import com.supportai.enums.RefundStatus;
import com.supportai.repository.OrderRepository;
import com.supportai.repository.RefundRepository;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiService {

    private static final Pattern ORDER_PATTERN = Pattern.compile("#?(\\d{4,})");

    private final OrderRepository orderRepository;
    private final RefundRepository refundRepository;
    private final RagService ragService;

    public AiService(
            OrderRepository orderRepository,
            RefundRepository refundRepository,
            RagService ragService
    ) {
        this.orderRepository = orderRepository;
        this.refundRepository = refundRepository;
        this.ragService = ragService;
    }

    public String generateReply(Conversation conversation, String userMessage) {
        String lowerMessage = userMessage.toLowerCase(Locale.ROOT);

        if (lowerMessage.contains("order") && (lowerMessage.contains("where") || lowerMessage.contains("status"))) {
            return handleOrderStatus(userMessage, conversation.getCompany().getId());
        }

        if (lowerMessage.contains("refund") && ORDER_PATTERN.matcher(userMessage).find()) {
            return handleRefundRequest(userMessage);
        }

        RagAnswerResponse response = ragService.answer(conversation.getCompany(), userMessage);
        return ragService.formatAnswerWithSources(response);
    }

    private String handleOrderStatus(String message, Long companyId) {
        Matcher matcher = ORDER_PATTERN.matcher(message);
        if (!matcher.find()) {
            return "I can check your order status. Please provide your order number (e.g. #48291).";
        }

        String orderNumber = matcher.group(1);
        return orderRepository.findByOrderNumberAndCompanyId(orderNumber, companyId)
                .map(this::formatOrderStatus)
                .orElse("I couldn't find order #" + orderNumber + ". Please double-check the number.");
    }

    private String formatOrderStatus(Order order) {
        StringBuilder response = new StringBuilder("Your order #" + order.getOrderNumber() + " is ");
        response.append(order.getStatus().name().toLowerCase().replace('_', ' '));

        if (order.getTrackingNumber() != null) {
            response.append(". Tracking: ").append(order.getTrackingNumber());
        }
        if (order.getExpectedDeliveryAt() != null) {
            response.append(". Expected delivery: ").append(order.getExpectedDeliveryAt());
        }
        return response.append(".").toString();
    }

    private String handleRefundRequest(String message) {
        Matcher matcher = ORDER_PATTERN.matcher(message);
        if (!matcher.find()) {
            return "I can help with that. Please provide your order number so I can submit the refund request.";
        }

        String orderNumber = matcher.group(1);
        return orderRepository.findByOrderNumber(orderNumber)
                .map(order -> {
                    Refund refund = new Refund();
                    refund.setOrder(order);
                    refund.setAmount(order.getTotalAmount());
                    refund.setReason("Customer requested refund via chat");
                    refund.setStatus(RefundStatus.REQUESTED);
                    refundRepository.save(refund);
                    return "I can help with that. Your refund for order #" + orderNumber
                            + " has been submitted and will be processed within 5 to 7 business days.";
                })
                .orElse("I couldn't find order #" + orderNumber + " to process a refund.");
    }
}
