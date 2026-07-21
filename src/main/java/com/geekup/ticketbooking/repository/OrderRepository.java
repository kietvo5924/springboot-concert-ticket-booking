package com.geekup.ticketbooking.repository;

import com.geekup.ticketbooking.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

import com.geekup.ticketbooking.enums.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByRequestId(String requestId);
    boolean existsByUserIdAndVoucherId(Long userId, Long voucherId);
    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime cutoffTime);
}
