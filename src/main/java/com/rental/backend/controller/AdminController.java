package com.rental.backend.controller;

import com.rental.backend.model.*;
import com.rental.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired private UserRepository userRepository;
    @Autowired private AgencyRepository agencyRepository;
    @Autowired private CarRepository carRepository;
    @Autowired private BookingRepository bookingRepository;

    // ==================== STATISTIQUES ====================
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalAgencies", agencyRepository.count());
        stats.put("totalCars", carRepository.count());
        stats.put("totalBookings", bookingRepository.count());
        
        // Revenue total de la plateforme
        Double totalRevenue = bookingRepository.getTotalRevenue();
        stats.put("totalRevenue", totalRevenue != null ? totalRevenue : 0.0);
        
        return ResponseEntity.ok(stats);
    }

    // ==================== REVENUS PAR AGENCE ====================
    @GetMapping("/revenue-by-agency")
    public ResponseEntity<List<Map<String, Object>>> getRevenueByAgency() {
        List<Agency> agencies = agencyRepository.findAll();
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        
        for (Agency agency : agencies) {
            Map<String, Object> agencyRevenue = new HashMap<>();
            agencyRevenue.put("agencyId", agency.getId());
            agencyRevenue.put("agencyName", agency.getName());
            agencyRevenue.put("city", agency.getCity());
            
            Double revenue = bookingRepository.getRevenueByAgencyId(agency.getId());
            agencyRevenue.put("revenue", revenue != null ? revenue : 0.0);
            
            Long totalBookings = bookingRepository.countByAgencyId(agency.getId());
            agencyRevenue.put("totalBookings", totalBookings);
            
            Long completedBookings = bookingRepository.countCompletedByAgencyId(agency.getId());
            agencyRevenue.put("completedBookings", completedBookings);
            
            result.add(agencyRevenue);
        }
        
        // Trier par revenu décroissant
        result.sort((a, b) -> Double.compare(
            (Double) b.get("revenue"), (Double) a.get("revenue")
        ));
        
        return ResponseEntity.ok(result);
    }

    // ==================== UTILISATEURS ====================
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Utilisateur supprimé avec succès"));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        User user = userRepository.findById(id)
                .orElse(null);
        
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        String newRole = payload.get("role");
        if (newRole == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le rôle est requis"));
        }

        try {
            user.setRole(Role.valueOf(newRole.toUpperCase()));
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Rôle modifié avec succès"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Rôle invalide"));
        }
    }

    @Autowired private PasswordEncoder passwordEncoder;

    @PutMapping("/users/{id}/password")
    public ResponseEntity<?> resetUserPassword(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        User user = userRepository.findById(id).orElse(null);
        
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        String newPassword = payload.get("password");
        if (newPassword == null || newPassword.length() < 4) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le mot de passe doit contenir au moins 4 caractères"));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        return ResponseEntity.ok(Map.of("message", "Mot de passe réinitialisé avec succès"));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // ==================== AGENCES ====================
    @GetMapping("/agencies")
    public ResponseEntity<List<Agency>> getAllAgencies() {
        List<Agency> agencies = agencyRepository.findAll();
        return ResponseEntity.ok(agencies);
    }

    @DeleteMapping("/agencies/{id}")
    public ResponseEntity<?> deleteAgency(@PathVariable Long id) {
        if (!agencyRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        agencyRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Agence supprimée avec succès"));
    }

    // ==================== VOITURES ====================
    @GetMapping("/cars")
    public ResponseEntity<List<Car>> getAllCars() {
        List<Car> cars = carRepository.findAll();
        return ResponseEntity.ok(cars);
    }

    @DeleteMapping("/cars/{id}")
    public ResponseEntity<?> deleteCar(@PathVariable Long id) {
        if (!carRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        carRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Voiture supprimée avec succès"));
    }

    // ==================== RÉSERVATIONS ====================
    @GetMapping("/bookings")
    public ResponseEntity<List<Booking>> getAllBookings() {
        List<Booking> bookings = bookingRepository.findAll();
        return ResponseEntity.ok(bookings);
    }

    @DeleteMapping("/bookings/{id}")
    public ResponseEntity<?> deleteBooking(@PathVariable Long id) {
        if (!bookingRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        bookingRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Réservation supprimée avec succès"));
    }

    @PutMapping("/bookings/{id}/status")
    public ResponseEntity<?> updateBookingStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        Booking booking = bookingRepository.findById(id).orElse(null);
        
        if (booking == null) {
            return ResponseEntity.notFound().build();
        }

        String newStatus = payload.get("status");
        if (newStatus == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le statut est requis"));
        }

        try {
            booking.setStatus(BookingStatus.valueOf(newStatus.toUpperCase()));
            bookingRepository.save(booking);
            return ResponseEntity.ok(Map.of("message", "Statut de réservation modifié avec succès"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Statut invalide"));
        }
    }
}
