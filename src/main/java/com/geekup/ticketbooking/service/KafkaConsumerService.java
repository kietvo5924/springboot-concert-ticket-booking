package com.geekup.ticketbooking.service;

import com.geekup.ticketbooking.message.BookingMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import org.springframework.kafka.core.KafkaTemplate;
import com.geekup.ticketbooking.message.FailedBookingMessage;
import com.geekup.ticketbooking.entity.DeadLetterMessage;
import com.geekup.ticketbooking.repository.DeadLetterMessageRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final OrderProcessorService orderProcessorService;
    private final RedissonClient redissonClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final DeadLetterMessageRepository deadLetterMessageRepository;

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
            
            // Push to Dead Letter Queue (DLQ)
            FailedBookingMessage failedMessage = new FailedBookingMessage(message, e.getMessage());
            kafkaTemplate.send("booking-requests-dlq", message.getRequestId(), failedMessage);
            log.info("Pushed failed request {} to DLQ topic.", message.getRequestId());
        }
    }

    @KafkaListener(topics = "booking-requests-dlq", groupId = "ticketbooking-dlq-group")
    public void consumeDlqRequest(FailedBookingMessage failedMessage) {
        BookingMessage msg = failedMessage.getOriginalMessage();
        
        // Prevent duplicate saving if already in DLQ
        if (deadLetterMessageRepository.findByRequestId(msg.getRequestId()).isPresent()) {
            return;
        }

        DeadLetterMessage dlqEntity = DeadLetterMessage.builder()
                .requestId(msg.getRequestId())
                .userId(msg.getUserId())
                .concertId(msg.getConcertId())
                .ticketCategoryId(msg.getTicketCategoryId())
                .voucherId(msg.getVoucherId())
                .errorMessage(failedMessage.getErrorMessage())
                .build();
        deadLetterMessageRepository.save(dlqEntity);
        log.warn("Saved failed request {} to DLQ database for Admin review.", msg.getRequestId());
    }
}
