package com.geekup.ticketbooking.service;

import com.geekup.ticketbooking.dto.TicketAvailabilityDto;
import com.geekup.ticketbooking.entity.Order;
import com.geekup.ticketbooking.entity.Ticket;
import com.geekup.ticketbooking.entity.TicketCategory;
import com.geekup.ticketbooking.enums.OrderStatus;
import com.geekup.ticketbooking.enums.TicketStatus;
import com.geekup.ticketbooking.repository.OrderRepository;
import com.geekup.ticketbooking.repository.TicketCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final OrderRepository orderRepository;
    private final TicketCategoryRepository ticketCategoryRepository;

    public Page<Order> getAllBookings(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    public List<TicketAvailabilityDto> getTicketAvailability() {
        List<TicketCategory> categories = ticketCategoryRepository.findAll();
        return categories.stream().map(cat -> TicketAvailabilityDto.builder()
                .categoryId(cat.getId())
                .categoryName(cat.getName())
                .initialQuantity(cat.getInitialQuantity())
                .remainingQuantity(cat.getRemainingQuantity())
                .soldQuantity(cat.getInitialQuantity() - cat.getRemainingQuantity())
                .build()).collect(Collectors.toList());
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        
        order.setStatus(newStatus);
        
        if (newStatus == OrderStatus.COMPLETED) {
            for (Ticket ticket : order.getTickets()) {
                ticket.setStatus(TicketStatus.SOLD);
            }
        } else if (newStatus == OrderStatus.CANCELLED || newStatus == OrderStatus.FAILED) {
            for (Ticket ticket : order.getTickets()) {
                ticket.setStatus(TicketStatus.AVAILABLE);
                ticket.setOrder(null);
            }
            // Trong thực tế, bạn cũng cần cộng lại remainingQuantity trong DB và Redis ở đây.
            // ticketCategory.setRemainingQuantity(ticketCategory.getRemainingQuantity() + 1);
        }
        
        return orderRepository.save(order);
    }
}
