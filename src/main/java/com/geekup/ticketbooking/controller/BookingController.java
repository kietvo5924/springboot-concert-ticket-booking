package com.geekup.ticketbooking.controller;

import com.geekup.ticketbooking.dto.BookingRequestDto;
import com.geekup.ticketbooking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<?> submitBooking(@Valid @RequestBody BookingRequestDto requestDto) {
        bookingService.submitBookingRequest(requestDto);
        return ResponseEntity.accepted().body(Map.of(
            "status", "ACCEPTED",
            "message", "Booking request received and is being processed.",
            "requestId", requestDto.getRequestId()
        ));
    }
}
