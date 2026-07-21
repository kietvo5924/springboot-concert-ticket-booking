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

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (concertRepository.count() == 0) {
            log.info("Seeding database with initial data...");

            Concert concert = Concert.builder()
                    .name("GeekUp Autumn Concert 2026")
                    .description("A spectacular musical event to celebrate autumn.")
                    .startTime(LocalDateTime.now().plusDays(30))
                    .endTime(LocalDateTime.now().plusDays(30).plusHours(3))
                    .location("HCMC Grand Theatre")
                    .build();
            concertRepository.save(concert);

            TicketCategory vipCategory = TicketCategory.builder()
                    .concert(concert)
                    .name("VIP")
                    .price(new BigDecimal("1500000"))
                    .initialQuantity(100)
                    .remainingQuantity(100)
                    .build();
            ticketCategoryRepository.save(vipCategory);

            for (int i = 1; i <= 100; i++) {
                Ticket ticket = Ticket.builder()
                        .ticketCategory(vipCategory)
                        .seatNumber("VIP-" + i)
                        .status(TicketStatus.AVAILABLE)
                        .build();
                ticketRepository.save(ticket);
            }

            // Sync inventory to Redis for Flash Sale
            RAtomicLong inventory = redissonClient.getAtomicLong("inventory:ticketCategory:" + vipCategory.getId());
            inventory.set(100);

            log.info("Seeding completed successfully! Concert ID: {}, VIP Category ID: {}", concert.getId(), vipCategory.getId());
        }
    }
}
