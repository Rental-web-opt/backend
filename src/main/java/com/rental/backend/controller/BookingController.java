package com.rental.backend.controller;

import com.rental.backend.model.Booking;
import com.rental.backend.model.BookingStatus;
import com.rental.backend.repository.BookingRepository;
import com.rental.backend.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    @Autowired private BookingService bookingService;
    @Autowired private BookingRepository bookingRepository;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Booking booking) {
        try {
            Booking created = bookingService.createBooking(booking);
            return ResponseEntity.ok(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", e.getMessage(),
                "error", true
            ));
        }
    }

    @GetMapping
    public List<Booking> getAll() {
        return bookingService.getAllBookings();
    }
    
    @GetMapping("/user/{userId}")
    public List<Booking> getByUser(@PathVariable Long userId) {
        return bookingService.getUserBookings(userId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Booking> getById(@PathVariable Long id) {
        return bookingRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Vérifie la disponibilité d'une voiture pour une période donnée
     * GET /api/bookings/check-availability?carId=1&startDate=2026-02-10T08:00:00&endDate=2026-02-15T18:00:00
     */
    @GetMapping("/check-availability")
    public ResponseEntity<?> checkAvailability(
            @RequestParam Long carId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        
        try {
            LocalDateTime start = LocalDateTime.parse(startDate);
            LocalDateTime end = LocalDateTime.parse(endDate);
            
            boolean available = bookingService.isCarAvailable(carId, start, end);
            
            if (available) {
                return ResponseEntity.ok(Map.of(
                    "available", true,
                    "message", "Le véhicule est disponible pour cette période"
                ));
            } else {
                List<Booking> conflicts = bookingService.getConflictingBookings(carId, start, end);
                return ResponseEntity.ok(Map.of(
                    "available", false,
                    "message", "Le véhicule n'est pas disponible pour cette période",
                    "conflictCount", conflicts.size()
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", true,
                "message", "Format de date invalide. Utilisez: yyyy-MM-ddTHH:mm:ss"
            ));
        }
    }

    /**
     * Récupère les créneaux occupés pour une voiture (pour afficher dans le calendrier)
     */
    @GetMapping("/car/{carId}/occupied")
    public ResponseEntity<?> getOccupiedSlots(@PathVariable Long carId) {
        List<Booking> upcomingBookings = bookingRepository.findUpcomingBookingsForCar(
            carId, LocalDateTime.now()
        );
        
        return ResponseEntity.ok(upcomingBookings.stream().map(b -> Map.of(
            "id", b.getId(),
            "startDate", b.getStartDate().toString(),
            "endDate", b.getEndDate().toString(),
            "status", b.getStatus().toString()
        )).toList());
    }

    /**
     * Confirmer une réservation (appelé après paiement réussi)
     */
    @PutMapping("/{id}/confirm")
    public ResponseEntity<?> confirmBooking(@PathVariable Long id) {
        Booking booking = bookingRepository.findById(id).orElse(null);
        
        if (booking == null) {
            return ResponseEntity.notFound().build();
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Cette réservation ne peut pas être confirmée. Statut actuel: " + booking.getStatus()));
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        
        return ResponseEntity.ok(Map.of(
            "message", "Réservation confirmée avec succès",
            "booking", booking
        ));
    }

    /**
     * Annuler une réservation
     * L'utilisateur ne peut annuler que SES propres réservations
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelBooking(
            @PathVariable Long id,
            @RequestParam Long userId) {
        try {
            Booking cancelled = bookingService.cancelBooking(id, userId);
            return ResponseEntity.ok(Map.of(
                "message", "Réservation annulée avec succès",
                "booking", cancelled
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", true,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Marquer une réservation comme terminée
     */
    @PutMapping("/{id}/complete")
    public ResponseEntity<?> completeBooking(@PathVariable Long id) {
        Booking booking = bookingRepository.findById(id).orElse(null);
        
        if (booking == null) {
            return ResponseEntity.notFound().build();
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Seules les réservations confirmées peuvent être marquées comme terminées"));
        }

        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);
        
        return ResponseEntity.ok(Map.of(
            "message", "Réservation terminée",
            "booking", booking
        ));
    }
}