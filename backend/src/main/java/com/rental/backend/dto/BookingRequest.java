package com.rental.backend.dto;

import lombok.Data;

@Data
public class BookingRequest {
    private Long carId;
    private Long userId;
    // NOUVEAU CHAMP
    private Long driverId; 
    
    private String startDate;
    private String endDate;
    private String rentalType;
    private boolean withDriver;
}