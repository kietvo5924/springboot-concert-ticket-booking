package com.geekup.ticketbooking.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingMessage implements Serializable {
    private String requestId;
    private Long userId;
    private Long concertId;
    private Long ticketCategoryId;
    private Long voucherId;
}
