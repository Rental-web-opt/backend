package com.rental.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cars")
public class Car {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String type;
    private String brand;
    private String model;
    
    // IMPORTANT : C'est ce nom que le BookingService cherche
    private Double pricePerDay; 
    private Double pricePerHour;
    
    private String location;
    private boolean isAvailable;
    
    @Column(length = 2000)
    private String description;

    // Images
    private String image;
    
    @ElementCollection
    private List<String> images;

    // Specs
    private String transmission;
    private String fuelType;
    private Integer seats;
    private Integer maxSpeed;
    
    @ManyToOne
    @JoinColumn(name = "agency_id")
    private Agency agency;
}