package com.geekup.ticketbooking.repository;

import com.geekup.ticketbooking.entity.DeadLetterMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeadLetterMessageRepository extends JpaRepository<DeadLetterMessage, Long> {
    Optional<DeadLetterMessage> findByRequestId(String requestId);
}
