package com.geekup.ticketbooking.service;

import com.geekup.ticketbooking.dto.BookingRequestDto;
import com.geekup.ticketbooking.exception.DuplicateRequestException;
import com.geekup.ticketbooking.exception.SoldOutException;
import com.geekup.ticketbooking.exception.SystemBusyException;
import com.geekup.ticketbooking.message.BookingMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final RedissonClient redissonClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final String KAFKA_TOPIC = "booking-requests";

    public void submitBookingRequest(BookingRequestDto requestDto) {
        String idempotencyKey = "idempotency:" + requestDto.getRequestId();
        
        // 1. Check Idempotency
        RBucket<String> bucket = redissonClient.getBucket(idempotencyKey);
        // Giữ key trong 5 phút để chống double-click hoặc retry
        if (!bucket.trySet("PROCESSING", 5, TimeUnit.MINUTES)) {
            log.warn("Duplicate request detected: {}", requestDto.getRequestId());
            throw new DuplicateRequestException("Request is already processed or processing");
        }

        // 2. Distributed Lock cho TicketCategory để chống race condition
        RLock lock = redissonClient.getLock("lock:ticketCategory:" + requestDto.getTicketCategoryId());
        try {
            // Chờ tối đa 5s để lấy lock, nếu lấy được thì giữ lock 5s
            boolean isLocked = lock.tryLock(5, 5, TimeUnit.SECONDS);
            if (!isLocked) {
                bucket.delete();
                log.error("Failed to acquire lock for ticket category: {}", requestDto.getTicketCategoryId());
                throw new SystemBusyException("System is busy, please try again later");
            }
            
            // 3. Kiểm tra số lượng vé tồn trong Redis
            RAtomicLong inventory = redissonClient.getAtomicLong("inventory:ticketCategory:" + requestDto.getTicketCategoryId());
            
            // Note: Trong thực tế, cần warmup Redis inventory từ DB trước khi chạy Flash Sale.
            // Nếu inventory chưa set, có thể fallback gọi DB ở đây, nhưng để tối ưu ta coi như đã có sẵn.
            
            if (inventory.get() <= 0) {
                bucket.delete(); // Cho phép user gửi request khác nếu muốn
                throw new SoldOutException("Tickets for this category are sold out");
            }
            
            // Trừ số lượng vé (Tentative decrement)
            inventory.decrementAndGet();
            
            // 4. Sinh Kafka Message đẩy vào topic
            BookingMessage message = new BookingMessage(
                requestDto.getRequestId(),
                requestDto.getUserId(),
                requestDto.getConcertId(),
                requestDto.getTicketCategoryId(),
                requestDto.getVoucherId()
            );
            
            kafkaTemplate.send(KAFKA_TOPIC, requestDto.getRequestId(), message);
            log.info("Successfully published booking request to Kafka. RequestId: {}", requestDto.getRequestId());
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            bucket.delete();
            throw new SystemBusyException("System interrupted, please try again");
        } finally {
            // Giải phóng lock
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
