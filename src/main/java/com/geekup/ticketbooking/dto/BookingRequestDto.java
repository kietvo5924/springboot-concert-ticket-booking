package com.geekup.ticketbooking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingRequestDto {
    @NotNull
    private Long userId;
    @NotNull
    private Long concertId;
    @NotNull
    private Long ticketCategoryId;
    private Long voucherId;
    @NotBlank
    private String requestId;
}
