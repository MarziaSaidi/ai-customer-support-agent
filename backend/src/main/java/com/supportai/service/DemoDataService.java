package com.supportai.service;

import com.supportai.entity.Company;
import com.supportai.entity.Order;
import com.supportai.enums.OrderStatus;
import com.supportai.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class DemoDataService {

    private final OrderRepository orderRepository;

    public DemoDataService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public void seedDemoOrder(Company company) {
        if (orderRepository.findByOrderNumberAndCompanyId("48291", company.getId()).isPresent()) {
            return;
        }

        Order order = new Order();
        order.setCompany(company);
        order.setOrderNumber("48291");
        order.setCustomerEmail("demo@customer.com");
        order.setStatus(OrderStatus.SHIPPED);
        order.setTotalAmount(new BigDecimal("129.99"));
        order.setShippingAddress("123 Demo Street, New York, NY 10001");
        order.setTrackingNumber("1Z999AA10123456784");
        order.setShippedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        order.setExpectedDeliveryAt(Instant.now().plus(2, ChronoUnit.DAYS));
        orderRepository.save(order);
    }
}
