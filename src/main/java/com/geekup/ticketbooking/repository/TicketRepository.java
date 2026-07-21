package com.geekup.ticketbooking.repository;

import com.geekup.ticketbooking.entity.Ticket;
import com.geekup.ticketbooking.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findFirstByTicketCategoryIdAndStatus(Long categoryId, TicketStatus status);
}
