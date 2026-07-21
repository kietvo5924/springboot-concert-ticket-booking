package com.geekup.ticketbooking.dto;

import com.geekup.ticketbooking.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateOrderStatusRequestDto {
    @NotNull
    private OrderStatus status;
}
