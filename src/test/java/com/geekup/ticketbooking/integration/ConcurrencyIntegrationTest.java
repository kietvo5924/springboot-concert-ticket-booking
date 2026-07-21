package com.geekup.ticketbooking.integration;

import com.geekup.ticketbooking.entity.*;
import com.geekup.ticketbooking.enums.TicketStatus;
import com.geekup.ticketbooking.message.BookingMessage;
import com.geekup.ticketbooking.repository.*;
import com.geekup.ticketbooking.service.OrderProcessorService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Slf4j
public class ConcurrencyIntegrationTest {

    @Autowired
    private OrderProcessorService orderProcessorService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private ConcertRepository concertRepository;

    @Autowired
    private TicketCategoryRepository ticketCategoryRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Concert concert;
    private TicketCategory category;
    private Voucher voucher;

    @BeforeEach
    void setUp() {
        // Clean up data for accurate testing
        orderRepository.deleteAll();
        ticketRepository.deleteAll();
        voucherRepository.deleteAll();
        ticketCategoryRepository.deleteAll();
        concertRepository.deleteAll();

        // Prepare base data
        concert = Concert.builder()
                .name("Test Concurrency Concert")
                .startTime(LocalDateTime.now().plusDays(10))
                .endTime(LocalDateTime.now().plusDays(10).plusHours(3))
                .build();
        concertRepository.save(concert);

        category = TicketCategory.builder()
                .concert(concert)
                .name("VIP")
                .price(new BigDecimal("1000000"))
                .initialQuantity(1)
                .remainingQuantity(1)
                .build();
        ticketCategoryRepository.save(category);
    }

    @Test
    void testOverselling_TwoUsersBuyingTheLastTicket() throws InterruptedException {
        // Arrange: ONLY 1 Ticket available
        Ticket ticket = Ticket.builder()
                .ticketCategory(category)
                .seatNumber("VIP-LAST")
                .status(TicketStatus.AVAILABLE)
                .build();
        ticketRepository.save(ticket);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // Act: 2 Concurrent Threads trying to process order for the SAME category
        Runnable buyTask = () -> {
            try {
                BookingMessage msg = new BookingMessage(UUID.randomUUID().toString(), 1L, concert.getId(), category.getId(), null);
                orderProcessorService.processFlashSaleOrder(msg);
                successCount.incrementAndGet();
            } catch (Exception e) {
                log.error("Failed to buy: {}", e.getMessage());
                failCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        };

        executorService.submit(buyTask);
        executorService.submit(buyTask);

        latch.await(5, TimeUnit.SECONDS);

        // Assert: Only 1 should succeed, 1 should fail because of PESSIMISTIC_WRITE lock and no physical tickets left
        assertEquals(1, successCount.get(), "Only 1 user should successfully get the ticket");
        assertEquals(1, failCount.get(), "The 2nd user must fail with Exception (No physical tickets available)");
        
        List<Order> orders = orderRepository.findAll();
        assertEquals(1, orders.size(), "Only 1 order should be saved in DB");
    }

    @Test
    void testVoucherAbuse_TwoUsersUsingTheLastVoucher() throws InterruptedException {
        // Arrange: 2 Tickets available, but ONLY 1 Voucher available
        ticketRepository.save(Ticket.builder().ticketCategory(category).seatNumber("VIP-1").status(TicketStatus.AVAILABLE).build());
        ticketRepository.save(Ticket.builder().ticketCategory(category).seatNumber("VIP-2").status(TicketStatus.AVAILABLE).build());

        voucher = Voucher.builder()
                .code("LAST_VOUCHER")
                .discountPercentage(new BigDecimal("50.0"))
                .quantity(1) // ONLY 1 VOUCHER!
                .active(true)
                .expiryDate(LocalDateTime.now().plusDays(1))
                .build();
        voucherRepository.save(voucher);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // Act: 2 Concurrent Threads trying to process order using the SAME voucher
        Runnable buyTask = () -> {
            try {
                // Different users, different request IDs, but same voucher
                Long randomUserId = (long) (Math.random() * 1000);
                BookingMessage msg = new BookingMessage(UUID.randomUUID().toString(), randomUserId, concert.getId(), category.getId(), voucher.getId());
                orderProcessorService.processFlashSaleOrder(msg);
                successCount.incrementAndGet();
            } catch (Exception e) {
                log.error("Failed to use voucher: {}", e.getMessage());
                failCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        };

        executorService.submit(buyTask);
        executorService.submit(buyTask);

        latch.await(5, TimeUnit.SECONDS);

        // Assert: Only 1 should succeed, 1 should fail because voucher quantity reached 0
        assertEquals(1, successCount.get(), "Only 1 user should successfully use the voucher");
        assertEquals(1, failCount.get(), "The 2nd user must fail with Exception (Voucher is fully consumed)");
        
        Voucher updatedVoucher = voucherRepository.findById(voucher.getId()).get();
        assertEquals(0, updatedVoucher.getQuantity(), "Voucher quantity should be exactly 0, not negative!");
    }

    @Test
    void testIdempotency_DuplicateBookingsCausedByRetries() throws InterruptedException {
        // Arrange: 2 Tickets available
        ticketRepository.save(Ticket.builder().ticketCategory(category).seatNumber("VIP-3").status(TicketStatus.AVAILABLE).build());
        ticketRepository.save(Ticket.builder().ticketCategory(category).seatNumber("VIP-4").status(TicketStatus.AVAILABLE).build());

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);

        // A single unique Request ID simulating a network retry or double click
        String duplicateRequestId = UUID.randomUUID().toString();
        BookingMessage msg = new BookingMessage(duplicateRequestId, 2L, concert.getId(), category.getId(), null);

        // Act: 2 Concurrent Threads processing the EXACT SAME message (Retry scenario)
        Runnable buyTask = () -> {
            try {
                orderProcessorService.processFlashSaleOrder(msg);
                successCount.incrementAndGet();
            } catch (Exception e) {
                log.error("Failed idempotency: {}", e.getMessage());
            } finally {
                latch.countDown();
            }
        };

        executorService.submit(buyTask);
        executorService.submit(buyTask);

        latch.await(5, TimeUnit.SECONDS);

        // Assert: Both methods might run without exception, but only ONE Order should be in the Database!
        List<Order> orders = orderRepository.findAll();
        assertEquals(1, orders.size(), "Idempotency failed! Duplicate orders created for the same Request ID.");
        assertEquals(duplicateRequestId, orders.get(0).getRequestId());
    }
}
