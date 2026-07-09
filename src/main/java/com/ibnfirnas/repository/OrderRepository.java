package com.ibnfirnas.repository;

import com.ibnfirnas.entity.Order;
import com.ibnfirnas.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
    Optional<Order> findByOrderNumber(String orderNumber);
    long countByStatus(OrderStatus status);
}