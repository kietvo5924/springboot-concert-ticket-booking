package com.geekup.ticketbooking.scheduler;

import com.geekup.ticketbooking.entity.Order;
import com.geekup.ticketbooking.entity.Ticket;
import com.geekup.ticketbooking.entity.TicketCategory;
import com.geekup.ticketbooking.entity.Voucher;
import com.geekup.ticketbooking.enums.OrderStatus;
import com.geekup.ticketbooking.enums.TicketStatus;
import com.geekup.ticketbooking.repository.OrderRepository;
import com.geekup.ticketbooking.repository.VoucherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TicketReleaseSchedulerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private RAtomicLong mockAtomicLong;

    @InjectMocks
    private TicketReleaseScheduler ticketReleaseScheduler;

    private Order expiredOrder;
    private Ticket reservedTicket;
    private Voucher usedVoucher;

    @BeforeEach
    void setUp() {
        TicketCategory category = TicketCategory.builder().build();
        category.setId(10L);

        reservedTicket = Ticket.builder()
                .ticketCategory(category)
                .status(TicketStatus.RESERVED)
                .build();

        usedVoucher = Voucher.builder()
                .quantity(5)
                .build();
        usedVoucher.setId(99L);

        expiredOrder = Order.builder()
                .status(OrderStatus.RESERVED)
                .tickets(Collections.singletonList(reservedTicket))
                .voucher(usedVoucher)
                .build();
        expiredOrder.setId(100L);
    }

    @Test
    void testReleaseUnpaidTickets_WithExpiredOrder_Success() {
        // Arrange
        when(orderRepository.findByStatusAndCreatedAtBefore(eq(OrderStatus.RESERVED), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(expiredOrder));
        when(redissonClient.getAtomicLong("inventory:ticketCategory:10")).thenReturn(mockAtomicLong);

        // Act
        ticketReleaseScheduler.releaseUnpaidTickets();

        // Assert
        assertEquals(OrderStatus.FAILED, expiredOrder.getStatus());
        assertEquals(TicketStatus.AVAILABLE, reservedTicket.getStatus());
        assertNull(reservedTicket.getOrder());
        
        // Verify inventory is refunded in Redis
        verify(mockAtomicLong, times(1)).incrementAndGet();
        
        // Verify voucher is refunded
        assertEquals(6, usedVoucher.getQuantity());
        verify(voucherRepository, times(1)).save(usedVoucher);
        
        // Verify order is saved with new FAILED status
        verify(orderRepository, times(1)).save(expiredOrder);
    }

    @Test
    void testReleaseUnpaidTickets_NoExpiredOrders() {
        // Arrange
        when(orderRepository.findByStatusAndCreatedAtBefore(eq(OrderStatus.RESERVED), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // Act
        ticketReleaseScheduler.releaseUnpaidTickets();

        // Assert
        verify(orderRepository, never()).save(any(Order.class));
        verify(redissonClient, never()).getAtomicLong(anyString());
        verify(voucherRepository, never()).save(any(Voucher.class));
    }
}
