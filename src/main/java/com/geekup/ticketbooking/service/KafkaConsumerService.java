package com.geekup.ticketbooking.service;

import com.geekup.ticketbooking.entity.*;
import com.geekup.ticketbooking.enums.OrderStatus;
import com.geekup.ticketbooking.enums.TicketStatus;
import com.geekup.ticketbooking.message.BookingMessage;
import com.geekup.ticketbooking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final ConcertRepository concertRepository;
    private final VoucherRepository voucherRepository;
    private final TicketCategoryRepository ticketCategoryRepository;
    private final RedissonClient redissonClient;

    @KafkaListener(topics = "booking-requests", groupId = "ticketbooking-group")
    @Transactional
    public void consumeBookingRequest(BookingMessage message) {
        log.info("Received booking request from Kafka: {}", message.getRequestId());

        try {
            if (orderRepository.findByRequestId(message.getRequestId()).isPresent()) {
                log.warn("Order with requestId {} already exists. Skipping.", message.getRequestId());
                return;
            }

            Concert concert = concertRepository.findById(message.getConcertId())
                    .orElseThrow(() -> new RuntimeException("Concert not found"));
                    
            TicketCategory category = ticketCategoryRepository.findById(message.getTicketCategoryId())
                    .orElseThrow(() -> new RuntimeException("Ticket Category not found"));

            Voucher voucher = null;
            BigDecimal discount = BigDecimal.ZERO;
            if (message.getVoucherId() != null) {
                voucher = voucherRepository.findById(message.getVoucherId())
                        .orElseThrow(() -> new RuntimeException("Voucher not found"));
                        
                if (voucher.getExpiryDate() != null && voucher.getExpiryDate().isBefore(LocalDateTime.now()) || !voucher.getActive()) {
                    throw new RuntimeException("Voucher is invalid or expired");
                }
                
                discount = category.getPrice().multiply(voucher.getDiscountPercentage()).divide(BigDecimal.valueOf(100));
                if (voucher.getMaxDiscountAmount() != null && discount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
                    discount = voucher.getMaxDiscountAmount();
                }
            }

            Ticket ticket = ticketRepository.findFirstByTicketCategoryIdAndStatus(category.getId(), TicketStatus.AVAILABLE)
                    .orElseThrow(() -> new RuntimeException("No physical tickets available"));

            BigDecimal finalAmount = category.getPrice().subtract(discount);
            if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
                finalAmount = BigDecimal.ZERO;
            }

            Order order = Order.builder()
                    .userId(message.getUserId())
                    .concert(concert)
                    .voucher(voucher)
                    .totalAmount(category.getPrice())
                    .discountAmount(discount)
                    .finalAmount(finalAmount)
                    .status(OrderStatus.RESERVED)
                    .requestId(message.getRequestId())
                    .build();

            ticket.setStatus(TicketStatus.RESERVED);
            ticket.setOrder(order);
            
            order.getTickets().add(ticket);
            
            orderRepository.save(order);
            log.info("Successfully created order {} for requestId {}", order.getId(), message.getRequestId());

        } catch (Exception e) {
            log.error("Error processing booking request {}. Rolling back inventory.", message.getRequestId(), e);
            RAtomicLong inventory = redissonClient.getAtomicLong("inventory:ticketCategory:" + message.getTicketCategoryId());
            inventory.incrementAndGet();
            throw e;
        }
    }
}
