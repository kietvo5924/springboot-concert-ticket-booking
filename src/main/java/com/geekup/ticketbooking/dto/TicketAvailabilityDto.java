package com.geekup.ticketbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketAvailabilityDto {
    private Long categoryId;
    private String categoryName;
    private Integer initialQuantity;
    private Integer remainingQuantity;
    private Integer soldQuantity;
}
