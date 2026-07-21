package com.geekup.ticketbooking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vouchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String code;

    private BigDecimal discountPercentage;
    
    private BigDecimal maxDiscountAmount;
    
    private LocalDateTime expiryDate;
    
    private Boolean active;
    
    private Integer quantity; // Limited voucher quantity
}
