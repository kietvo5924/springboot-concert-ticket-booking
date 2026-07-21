package com.geekup.ticketbooking.service;

import com.geekup.ticketbooking.message.BookingMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final OrderProcessorService orderProcessorService;
    private final RedissonClient redissonClient;

    @KafkaListener(topics = "booking-requests", groupId = "ticketbooking-group")
    public void consumeBookingRequest(BookingMessage message) {
        log.info("Received booking request from Kafka: {}", message.getRequestId());

        try {
            orderProcessorService.processFlashSaleOrder(message);
        } catch (Exception e) {
            log.error("Error processing booking request {}. Rolling back inventory.", message.getRequestId(), e);
            // Rollback inventory in Redis
            RAtomicLong inventory = redissonClient.getAtomicLong("inventory:ticketCategory:" + message.getTicketCategoryId());
            inventory.incrementAndGet();
            // Note: Should also push to a Dead Letter Queue (DLQ) in a production environment
        }
    }
}
