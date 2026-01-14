package com.rental.backend.model;

import jakarta.persistence.*;
import lombok.Data; // <--- C'est lui qui génère les getters/setters
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data // <--- INDISPENSABLE
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "car_id")
    private Car car; // Génère getCar() et setCar()

    private Long userId;
    private boolean withDriver; // Génère isWithDriver() et setWithDriver()
    
    private LocalDateTime startDate; // Génère getStartDate()
    private LocalDateTime endDate;   // Génère getEndDate()

    private Double totalPrice; // Génère setTotalPrice()
    private String rentalType;

    @Enumerated(EnumType.STRING)
    private BookingStatus status; // Génère setStatus()

    private LocalDateTime createdAt = LocalDateTime.now();
}