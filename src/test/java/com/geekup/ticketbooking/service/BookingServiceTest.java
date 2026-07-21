package com.geekup.ticketbooking.service;

import com.geekup.ticketbooking.dto.BookingRequestDto;
import com.geekup.ticketbooking.exception.DuplicateRequestException;
import com.geekup.ticketbooking.exception.SoldOutException;
import com.geekup.ticketbooking.exception.SystemBusyException;
import com.geekup.ticketbooking.message.BookingMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private RBucket<String> bucket;

    @Mock
    private RLock lock;

    @Mock
    private RAtomicLong inventory;

    @InjectMocks
    private BookingService bookingService;

    private BookingRequestDto requestDto;

    @BeforeEach
    void setUp() {
        requestDto = new BookingRequestDto();
        requestDto.setUserId(1L);
        requestDto.setConcertId(10L);
        requestDto.setTicketCategoryId(100L);
        requestDto.setVoucherId(1000L);
        requestDto.setRequestId("req-123");
    }

    @Test
    void submitBookingRequest_Success() throws InterruptedException {
        // Arrange
        when(redissonClient.<String>getBucket("idempotency:req-123")).thenReturn(bucket);
        when(bucket.trySet(eq("PROCESSING"), eq(5L), eq(TimeUnit.MINUTES))).thenReturn(true);
        
        when(redissonClient.getLock("lock:ticketCategory:100")).thenReturn(lock);
        when(lock.tryLock(5, 5, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isLocked()).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        when(redissonClient.getAtomicLong("inventory:ticketCategory:100")).thenReturn(inventory);
        when(inventory.get()).thenReturn(10L);

        // Act
        bookingService.submitBookingRequest(requestDto);

        // Assert
        verify(inventory, times(1)).decrementAndGet();
        ArgumentCaptor<BookingMessage> messageCaptor = ArgumentCaptor.forClass(BookingMessage.class);
        verify(kafkaTemplate, times(1)).send(eq("booking-requests"), eq("req-123"), messageCaptor.capture());
        
        BookingMessage capturedMsg = messageCaptor.getValue();
        assertEquals("req-123", capturedMsg.getRequestId());
        assertEquals(1L, capturedMsg.getUserId());
        assertEquals(10L, capturedMsg.getConcertId());
        
        verify(lock, times(1)).unlock();
    }

    @Test
    void submitBookingRequest_DuplicateRequestId_ThrowsException() {
        // Arrange
        when(redissonClient.<String>getBucket("idempotency:req-123")).thenReturn(bucket);
        when(bucket.trySet(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        // Act & Assert
        DuplicateRequestException exception = assertThrows(DuplicateRequestException.class, () -> {
            bookingService.submitBookingRequest(requestDto);
        });

        assertEquals("Request is already processed or processing", exception.getMessage());
        verify(redissonClient, never()).getLock(anyString());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void submitBookingRequest_SoldOut_ThrowsException() throws InterruptedException {
        // Arrange
        when(redissonClient.<String>getBucket("idempotency:req-123")).thenReturn(bucket);
        when(bucket.trySet(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        
        when(redissonClient.getLock("lock:ticketCategory:100")).thenReturn(lock);
        when(lock.tryLock(5, 5, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isLocked()).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        when(redissonClient.getAtomicLong("inventory:ticketCategory:100")).thenReturn(inventory);
        when(inventory.get()).thenReturn(0L); // Sold out

        // Act & Assert
        SoldOutException exception = assertThrows(SoldOutException.class, () -> {
            bookingService.submitBookingRequest(requestDto);
        });

        assertEquals("Tickets for this category are sold out", exception.getMessage());
        verify(bucket, times(1)).delete();
        verify(inventory, never()).decrementAndGet();
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        verify(lock, times(1)).unlock();
    }
}
