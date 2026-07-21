package com.geekup.ticketbooking.controller;

import com.geekup.ticketbooking.dto.TicketAvailabilityDto;
import com.geekup.ticketbooking.dto.UpdateOrderStatusRequestDto;
import com.geekup.ticketbooking.entity.Order;
import com.geekup.ticketbooking.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/bookings")
    public ResponseEntity<Page<Order>> getAllBookings(Pageable pageable) {
        return ResponseEntity.ok(adminService.getAllBookings(pageable));
    }

    @GetMapping("/tickets/availability")
    public ResponseEntity<List<TicketAvailabilityDto>> getTicketAvailability() {
        return ResponseEntity.ok(adminService.getTicketAvailability());
    }

    @PutMapping("/bookings/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequestDto request) {
        Order updatedOrder = adminService.updateOrderStatus(id, request.getStatus());
        return ResponseEntity.ok(updatedOrder);
    }
}
