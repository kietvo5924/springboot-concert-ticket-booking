package com.geekup.ticketbooking.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketCategoryResponseDto {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer initialQuantity;
    private Integer remainingQuantity;
}
