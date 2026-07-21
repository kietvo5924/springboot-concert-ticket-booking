package com.geekup.ticketbooking.service;

import com.geekup.ticketbooking.entity.*;
import com.geekup.ticketbooking.enums.TicketStatus;
import com.geekup.ticketbooking.message.BookingMessage;
import com.geekup.ticketbooking.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderProcessorServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private ConcertRepository concertRepository;

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private TicketCategoryRepository ticketCategoryRepository;

    @InjectMocks
    private OrderProcessorService orderProcessorService;

    private BookingMessage bookingMessage;
    private Concert concert;
    private TicketCategory category;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        bookingMessage = new BookingMessage("req-123", 1L, 10L, 100L, null);

        concert = Concert.builder().id(10L).name("Test Concert").build();
        category = TicketCategory.builder().id(100L).concert(concert).price(new BigDecimal("100000")).build();
        ticket = Ticket.builder().id(999L).ticketCategory(category).status(TicketStatus.AVAILABLE).build();
    }

    @Test
    void testProcessOrder_Success_NoVoucher() {
        // Arrange
        when(orderRepository.findByRequestId("req-123")).thenReturn(Optional.empty());
        when(concertRepository.findById(10L)).thenReturn(Optional.of(concert));
        when(ticketCategoryRepository.findById(100L)).thenReturn(Optional.of(category));
        when(ticketRepository.findFirstByTicketCategoryIdAndStatus(100L, TicketStatus.AVAILABLE)).thenReturn(Optional.of(ticket));

        // Act
        orderProcessorService.processFlashSaleOrder(bookingMessage);

        // Assert
        verify(orderRepository, times(1)).save(any(Order.class));
        assertEquals(TicketStatus.RESERVED, ticket.getStatus());
    }

    @Test
    void testProcessOrder_DuplicateRequest_ShouldSkip() {
        // Arrange
        when(orderRepository.findByRequestId("req-123")).thenReturn(Optional.of(new Order()));

        // Act
        orderProcessorService.processFlashSaleOrder(bookingMessage);

        // Assert
        verify(concertRepository, never()).findById(anyLong());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testProcessOrder_WithValidVoucher_Success() {
        // Arrange
        bookingMessage.setVoucherId(50L);
        Voucher voucher = Voucher.builder()
                .id(50L)
                .code("DISCOUNT10")
                .active(true)
                .quantity(10)
                .discountPercentage(new BigDecimal("10.0"))
                .expiryDate(LocalDateTime.now().plusDays(1))
                .build();

        when(orderRepository.findByRequestId("req-123")).thenReturn(Optional.empty());
        when(concertRepository.findById(10L)).thenReturn(Optional.of(concert));
        when(ticketCategoryRepository.findById(100L)).thenReturn(Optional.of(category));
        when(voucherRepository.findWithLockById(50L)).thenReturn(Optional.of(voucher));
        when(orderRepository.existsByUserIdAndVoucherId(1L, 50L)).thenReturn(false);
        when(ticketRepository.findFirstByTicketCategoryIdAndStatus(100L, TicketStatus.AVAILABLE)).thenReturn(Optional.of(ticket));

        // Act
        orderProcessorService.processFlashSaleOrder(bookingMessage);

        // Assert
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(voucherRepository, times(1)).save(voucher);
        assertEquals(9, voucher.getQuantity()); // Quantity decreased
    }

    @Test
    void testProcessOrder_WithFullyConsumedVoucher_ShouldThrowException() {
        // Arrange
        bookingMessage.setVoucherId(50L);
        Voucher voucher = Voucher.builder().id(50L).active(true).quantity(0).build();

        when(orderRepository.findByRequestId("req-123")).thenReturn(Optional.empty());
        when(concertRepository.findById(10L)).thenReturn(Optional.of(concert));
        when(ticketCategoryRepository.findById(100L)).thenReturn(Optional.of(category));
        when(voucherRepository.findWithLockById(50L)).thenReturn(Optional.of(voucher));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> orderProcessorService.processFlashSaleOrder(bookingMessage));
        assertEquals("Voucher is fully consumed", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }
}
