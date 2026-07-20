package com.supportai.service;

import com.supportai.entity.Company;
import com.supportai.entity.Conversation;
import com.supportai.entity.Order;
import com.supportai.entity.Ticket;
import com.supportai.enums.OrderStatus;
import com.supportai.enums.TicketStatus;
import com.supportai.repository.ConversationRepository;
import com.supportai.repository.OrderRepository;
import com.supportai.repository.RefundRepository;
import com.supportai.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiFunctionServiceUnitTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private RagService ragService;

    @InjectMocks
    private AiFunctionService aiFunctionService;

    @Test
    void checkOrderStatusReturnsShippingDetails() {
        Order order = new Order();
        order.setOrderNumber("48291");
        order.setStatus(OrderStatus.SHIPPED);
        order.setTrackingNumber("1Z999AA10123456784");

        when(orderRepository.findByOrderNumberAndCompanyId("48291", 1L)).thenReturn(Optional.of(order));

        String result = aiFunctionService.checkOrderStatus("Where is order #48291?", 1L);

        assertTrue(result.contains("48291"));
        assertTrue(result.toLowerCase().contains("shipped"));
        assertTrue(result.contains("1Z999AA10123456784"));
    }

    @Test
    void cancelOrderRejectsShippedOrders() {
        Order order = new Order();
        order.setOrderNumber("48291");
        order.setStatus(OrderStatus.SHIPPED);

        when(orderRepository.findByOrderNumberAndCompanyId("48291", 1L)).thenReturn(Optional.of(order));

        String result = aiFunctionService.cancelOrder("48291", 1L);

        assertTrue(result.contains("cannot be cancelled"));
    }

    @Test
    void cancelOrderCancelsPendingOrders() {
        Order order = new Order();
        order.setOrderNumber("90001");
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("50.00"));

        when(orderRepository.findByOrderNumberAndCompanyId("90001", 1L)).thenReturn(Optional.of(order));

        String result = aiFunctionService.cancelOrder("90001", 1L);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertTrue(result.contains("cancelled successfully"));
        verify(orderRepository).save(order);
    }

    @Test
    void createTicketLinksConversationAndCustomer() {
        Company company = new Company();
        company.setId(1L);
        company.setName("Acme");

        Conversation conversation = new Conversation();
        conversation.setId(10L);
        conversation.setCompany(company);
        conversation.setCustomerEmail("buyer@test.com");

        when(conversationRepository.findByIdAndCompanyId(10L, 1L)).thenReturn(Optional.of(conversation));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            ticket.setId(99L);
            return ticket;
        });

        String result = aiFunctionService.createTicket(
                "Damaged item",
                "Box arrived torn",
                1L,
                10L,
                null,
                "HIGH"
        );

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());

        Ticket saved = captor.getValue();
        assertEquals("Damaged item", saved.getSubject());
        assertEquals(TicketStatus.OPEN, saved.getStatus());
        assertEquals("buyer@test.com", saved.getCustomerEmail());
        assertTrue(result.contains("ticket #99"));
    }
}
