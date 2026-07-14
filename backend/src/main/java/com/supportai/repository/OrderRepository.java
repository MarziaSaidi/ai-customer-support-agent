package com.supportai.repository;

import com.supportai.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumberAndCompanyId(String orderNumber, Long companyId);
    Optional<Order> findByOrderNumber(String orderNumber);
}
