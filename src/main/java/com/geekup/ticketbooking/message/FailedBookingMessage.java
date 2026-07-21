package com.geekup.ticketbooking.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FailedBookingMessage implements Serializable {
    private BookingMessage originalMessage;
    private String errorMessage;
}
