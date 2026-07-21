package com.geekup.ticketbooking.controller;

import com.geekup.ticketbooking.dto.OrderResponseDto;
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
    public ResponseEntity<Page<OrderResponseDto>> getAllBookings(Pageable pageable) {
        Page<Order> orders = adminService.getAllBookings(pageable);
        Page<OrderResponseDto> response = orders.map(this::mapToOrderResponseDto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tickets/availability")
    public ResponseEntity<List<TicketAvailabilityDto>> getTicketAvailability() {
        return ResponseEntity.ok(adminService.getTicketAvailability());
    }

    @PutMapping("/bookings/{id}/status")
    public ResponseEntity<OrderResponseDto> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequestDto request) {
        Order updatedOrder = adminService.updateOrderStatus(id, request.getStatus());
        return ResponseEntity.ok(mapToOrderResponseDto(updatedOrder));
    }
    
    private OrderResponseDto mapToOrderResponseDto(Order order) {
        return OrderResponseDto.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .concertId(order.getConcert() != null ? order.getConcert().getId() : null)
                .concertName(order.getConcert() != null ? order.getConcert().getName() : null)
                .voucherId(order.getVoucher() != null ? order.getVoucher().getId() : null)
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .finalAmount(order.getFinalAmount())
                .status(order.getStatus())
                .requestId(order.getRequestId())
                .build();
    }
}
