package com.supportai.service;

import com.supportai.dto.RagAnswerResponse;
import com.supportai.entity.Company;
import com.supportai.entity.Conversation;
import com.supportai.entity.Order;
import com.supportai.entity.Refund;
import com.supportai.entity.Ticket;
import com.supportai.enums.OrderStatus;
import com.supportai.enums.RefundStatus;
import com.supportai.enums.TicketPriority;
import com.supportai.enums.TicketStatus;
import com.supportai.repository.ConversationRepository;
import com.supportai.repository.OrderRepository;
import com.supportai.repository.RefundRepository;
import com.supportai.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AiFunctionService {

    private static final Pattern ORDER_NUMBER_PATTERN = Pattern.compile("\\D*(\\d{4,})");

    private final OrderRepository orderRepository;
    private final RefundRepository refundRepository;
    private final TicketRepository ticketRepository;
    private final ConversationRepository conversationRepository;
    private final RagService ragService;

    public AiFunctionService(
            OrderRepository orderRepository,
            RefundRepository refundRepository,
            TicketRepository ticketRepository,
            ConversationRepository conversationRepository,
            RagService ragService
    ) {
        this.orderRepository = orderRepository;
        this.refundRepository = refundRepository;
        this.ticketRepository = ticketRepository;
        this.conversationRepository = conversationRepository;
        this.ragService = ragService;
    }

    public String checkOrderStatus(String orderNumber, Long companyId) {
        String normalized = normalizeOrderNumber(orderNumber);
        if (normalized == null) {
            return "Please provide a valid order number (e.g. 48291).";
        }

        return orderRepository.findByOrderNumberAndCompanyId(normalized, companyId)
                .map(this::formatOrderStatus)
                .orElse("I couldn't find order #" + normalized + ". Please double-check the number.");
    }

    @Transactional
    public String createTicket(
            String subject,
            String description,
            Long companyId,
            Long conversationId,
            String customerEmail,
            String priority
    ) {
        Conversation conversation = conversationRepository.findByIdAndCompanyId(conversationId, companyId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        Ticket ticket = new Ticket();
        ticket.setCompany(conversation.getCompany());
        ticket.setConversation(conversation);
        ticket.setSubject(subject);
        ticket.setDescription(description);
        ticket.setCustomerEmail(customerEmail != null ? customerEmail : conversation.getCustomerEmail());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(parsePriority(priority));
        ticketRepository.save(ticket);

        return "Support ticket #" + ticket.getId() + " has been created. "
                + "A human agent will follow up on: " + subject + ".";
    }

    @Transactional
    public String cancelOrder(String orderNumber, Long companyId) {
        String normalized = normalizeOrderNumber(orderNumber);
        if (normalized == null) {
            return "Please provide a valid order number to cancel.";
        }

        return orderRepository.findByOrderNumberAndCompanyId(normalized, companyId)
                .map(order -> {
                    if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
                        return "Order #" + normalized + " has already shipped and cannot be cancelled. "
                                + "I can create a return ticket or request a refund instead.";
                    }
                    if (order.getStatus() == OrderStatus.CANCELLED) {
                        return "Order #" + normalized + " is already cancelled.";
                    }
                    order.setStatus(OrderStatus.CANCELLED);
                    orderRepository.save(order);
                    return "Order #" + normalized + " has been cancelled successfully.";
                })
                .orElse("I couldn't find order #" + normalized + " to cancel.");
    }

    @Transactional
    public String requestRefund(String orderNumber, Long companyId) {
        String normalized = normalizeOrderNumber(orderNumber);
        if (normalized == null) {
            return "Please provide your order number so I can submit the refund request.";
        }

        return orderRepository.findByOrderNumberAndCompanyId(normalized, companyId)
                .map(order -> {
                    Refund refund = new Refund();
                    refund.setOrder(order);
                    refund.setAmount(order.getTotalAmount());
                    refund.setReason("Customer requested refund via chat");
                    refund.setStatus(RefundStatus.REQUESTED);
                    refundRepository.save(refund);
                    return "Your refund for order #" + normalized
                            + " has been submitted and will be processed within 5 to 7 business days.";
                })
                .orElse("I couldn't find order #" + normalized + " to process a refund.");
    }

    public String searchDocumentation(Company company, String query) {
        RagAnswerResponse response = ragService.answer(company, query);
        return ragService.formatAnswerWithSources(response);
    }

    private String formatOrderStatus(Order order) {
        StringBuilder response = new StringBuilder("Order #" + order.getOrderNumber() + " is ");
        response.append(order.getStatus().name().toLowerCase(Locale.ROOT).replace('_', ' '));

        if (order.getTrackingNumber() != null) {
            response.append(". Tracking: ").append(order.getTrackingNumber());
        }
        if (order.getExpectedDeliveryAt() != null) {
            response.append(". Expected delivery: ").append(order.getExpectedDeliveryAt());
        }
        return response.append(".").toString();
    }

    private TicketPriority parsePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return TicketPriority.MEDIUM;
        }
        try {
            return TicketPriority.valueOf(priority.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return TicketPriority.MEDIUM;
        }
    }

    private String normalizeOrderNumber(String orderNumber) {
        if (orderNumber == null || orderNumber.isBlank()) {
            return null;
        }
        var matcher = ORDER_NUMBER_PATTERN.matcher(orderNumber.trim());
        return matcher.find() ? matcher.group(1) : null;
    }
}
