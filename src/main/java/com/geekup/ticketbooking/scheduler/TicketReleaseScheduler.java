package com.geekup.ticketbooking.scheduler;

import com.geekup.ticketbooking.entity.Order;
import com.geekup.ticketbooking.entity.Ticket;
import com.geekup.ticketbooking.enums.OrderStatus;
import com.geekup.ticketbooking.enums.TicketStatus;
import com.geekup.ticketbooking.repository.OrderRepository;
import com.geekup.ticketbooking.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class TicketReleaseScheduler {

    private final OrderRepository orderRepository;
    private final RedissonClient redissonClient;
    private final VoucherRepository voucherRepository;

    @Scheduled(fixedRate = 60000) // Runs every minute
    @Transactional
    public void releaseUnpaidTickets() {
        log.info("Starting cronjob to release unpaid tickets...");
        // Define cutoff time (e.g., 1 minute for testing, usually 15 mins in prod)
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(1);

        List<Order> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.RESERVED, cutoffTime);

        if (expiredOrders.isEmpty()) {
            return;
        }

        for (Order order : expiredOrders) {
            log.info("Releasing tickets for expired order: {}", order.getId());
            order.setStatus(OrderStatus.FAILED);

            for (Ticket ticket : order.getTickets()) {
                ticket.setStatus(TicketStatus.AVAILABLE);
                ticket.setOrder(null);

                // Refund inventory to Redis
                redissonClient.getAtomicLong("inventory:ticketCategory:" + ticket.getTicketCategory().getId())
                        .incrementAndGet();
                        
                // Refund inventory to Database TicketCategory
                com.geekup.ticketbooking.entity.TicketCategory category = ticket.getTicketCategory();
                category.setRemainingQuantity(category.getRemainingQuantity() + 1);
                // Note: It will be saved automatically by JPA cascade or at the end of transaction, 
                // but let's be explicit if we need to or just let JPA handle it.
                // ticketCategoryRepository.save(category) is needed if not managed or cascaded.
            }

            // Refund voucher if used
            if (order.getVoucher() != null && order.getVoucher().getQuantity() != null) {
                order.getVoucher().setQuantity(order.getVoucher().getQuantity() + 1);
                voucherRepository.save(order.getVoucher());
            }

            orderRepository.save(order);
        }

        log.info("Finished releasing unpaid tickets. Total orders cancelled: {}", expiredOrders.size());
    }
}
