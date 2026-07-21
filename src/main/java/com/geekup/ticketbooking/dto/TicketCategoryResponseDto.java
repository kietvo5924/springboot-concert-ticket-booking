package com.geekup.ticketbooking.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TicketCategoryResponseDto {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer initialQuantity;
    private Integer remainingQuantity;
}
