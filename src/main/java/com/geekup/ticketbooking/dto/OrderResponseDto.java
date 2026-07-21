package com.geekup.ticketbooking.dto;

import com.geekup.ticketbooking.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderResponseDto {
    private Long id;
    private Long userId;
    private Long concertId;
    private String concertName;
    private Long voucherId;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private OrderStatus status;
    private String requestId;
}
