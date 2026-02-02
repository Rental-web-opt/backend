package com.rental.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;

    private Long userId;
    private Double amount;
    private String currency = "XAF";
    
    // CARD, MTN, ORANGE
    private String paymentMethod;
    
    // PENDING, COMPLETED, FAILED, REFUNDED
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    
    // Référence externe (Stripe, MoMo, etc.)
    private String transactionReference;
    
    private String description;
    
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime completedAt;
}
