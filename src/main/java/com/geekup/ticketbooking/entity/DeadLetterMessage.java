package com.geekup.ticketbooking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "dead_letter_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeadLetterMessage extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String requestId;

    private Long userId;
    private Long concertId;
    private Long ticketCategoryId;
    private Long voucherId;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;
}
