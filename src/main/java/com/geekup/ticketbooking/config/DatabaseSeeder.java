package com.geekup.ticketbooking.config;

import com.geekup.ticketbooking.entity.Concert;
import com.geekup.ticketbooking.entity.Ticket;
import com.geekup.ticketbooking.entity.TicketCategory;
import com.geekup.ticketbooking.enums.TicketStatus;
import com.geekup.ticketbooking.repository.ConcertRepository;
import com.geekup.ticketbooking.repository.TicketCategoryRepository;
import com.geekup.ticketbooking.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final ConcertRepository concertRepository;
    private final TicketCategoryRepository ticketCategoryRepository;
    private final TicketRepository ticketRepository;
    private final RedissonClient redissonClient;

    private final com.geekup.ticketbooking.repository.VoucherRepository voucherRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (concertRepository.count() == 0) {
            log.info("Seeding database with robust initial data for testing...");

            // --- 1. Seed Concerts ---
            Concert concert1 = Concert.builder()
                    .name("GeekUp Autumn Concert 2026")
                    .description("A spectacular musical event to celebrate autumn.")
                    .startTime(LocalDateTime.now().plusDays(30))
                    .endTime(LocalDateTime.now().plusDays(30).plusHours(3))
                    .location("HCMC Grand Theatre")
                    .build();

            Concert concert2 = Concert.builder()
                    .name("Indie Rock Night 2026")
                    .description("The best indie bands gathered in one night.")
                    .startTime(LocalDateTime.now().plusDays(15))
                    .endTime(LocalDateTime.now().plusDays(15).plusHours(4))
                    .location("Hoa Binh Theatre")
                    .build();
                    
            concertRepository.saveAll(java.util.List.of(concert1, concert2));

            // --- 2. Seed Ticket Categories ---
            TicketCategory c1Vip = createCategory(concert1, "VIP", new BigDecimal("1500000"), 50);
            TicketCategory c1Standard = createCategory(concert1, "STANDARD", new BigDecimal("800000"), 150);
            TicketCategory c2General = createCategory(concert2, "GENERAL", new BigDecimal("350000"), 300);
            
            ticketCategoryRepository.saveAll(java.util.List.of(c1Vip, c1Standard, c2General));

            // --- 3. Seed Tickets & Redis Inventory ---
            generateTicketsAndSyncRedis(c1Vip);
            generateTicketsAndSyncRedis(c1Standard);
            generateTicketsAndSyncRedis(c2General);

            // --- 4. Seed Vouchers ---
            com.geekup.ticketbooking.entity.Voucher voucher1 = com.geekup.ticketbooking.entity.Voucher.builder()
                    .code("GEEKUP2026")
                    .discountPercentage(new BigDecimal("10.0")) // 10% off
                    .maxDiscountAmount(new BigDecimal("200000")) // up to 200k
                    .expiryDate(LocalDateTime.now().plusDays(60))
                    .active(true)
                    .quantity(100)
                    .build();
            
            com.geekup.ticketbooking.entity.Voucher voucher2 = com.geekup.ticketbooking.entity.Voucher.builder()
                    .code("FLASH50")
                    .discountPercentage(new BigDecimal("50.0")) // 50% off
                    .maxDiscountAmount(new BigDecimal("500000")) 
                    .expiryDate(LocalDateTime.now().plusDays(60))
                    .active(true)
                    .quantity(10)
                    .build();

            voucherRepository.saveAll(java.util.List.of(voucher1, voucher2));

            log.info("Robust seeding completed successfully!");
        } else {
            log.info("Database already seeded. Syncing inventory to Redis...");
            for (TicketCategory category : ticketCategoryRepository.findAll()) {
                RAtomicLong inventory = redissonClient.getAtomicLong("inventory:ticketCategory:" + category.getId());
                long availableCount = ticketRepository.countByTicketCategoryIdAndStatus(category.getId(), TicketStatus.AVAILABLE);
                inventory.set(availableCount);
            }
            log.info("Redis inventory sync completed.");
        }
    }

    private TicketCategory createCategory(Concert concert, String name, BigDecimal price, int quantity) {
        return TicketCategory.builder()
                .concert(concert)
                .name(name)
                .price(price)
                .initialQuantity(quantity)
                .remainingQuantity(quantity)
                .build();
    }

    private void generateTicketsAndSyncRedis(TicketCategory category) {
        java.util.List<Ticket> tickets = new java.util.ArrayList<>();
        for (int i = 1; i <= category.getInitialQuantity(); i++) {
            tickets.add(Ticket.builder()
                    .ticketCategory(category)
                    .seatNumber(category.getName() + "-" + i)
                    .status(TicketStatus.AVAILABLE)
                    .build());
        }
        ticketRepository.saveAll(tickets);

        // Sync inventory to Redis for Flash Sale
        RAtomicLong inventory = redissonClient.getAtomicLong("inventory:ticketCategory:" + category.getId());
        inventory.set(category.getInitialQuantity());
    }
}
