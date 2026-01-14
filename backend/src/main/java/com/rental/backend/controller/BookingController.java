package com.rental.backend.controller;

import com.rental.backend.dto.BookingRequest; // <--- Import Important
import com.rental.backend.model.Booking;
import com.rental.backend.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "http://localhost:3000")
public class BookingController {
    @Autowired private BookingService bookingService;

    // Utilisation du DTO ici
    @PostMapping
    public Booking create(@RequestBody BookingRequest request) {
        return bookingService.createBooking(request);
    }

    @GetMapping
    public List<Booking> getAll() {
        return bookingService.getAllBookings();
    }
    
    @GetMapping("/user/{userId}")
    public List<Booking> getByUser(@PathVariable Long userId) {
        return bookingService.getUserBookings(userId);
    }
    // Endpoint pour modifier le statut (CONFIRMED, CANCELLED)
    @PutMapping("/{id}/status")
    public ResponseEntity<Booking> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(bookingService.updateBookingStatus(id, status));
    }
}