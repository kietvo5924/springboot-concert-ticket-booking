package com.geekup.ticketbooking.repository;

import com.geekup.ticketbooking.entity.Ticket;
import com.geekup.ticketbooking.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    Optional<Ticket> findFirstByTicketCategoryIdAndStatus(Long categoryId, TicketStatus status);
    
    long countByTicketCategoryIdAndStatus(Long categoryId, TicketStatus status);
}
