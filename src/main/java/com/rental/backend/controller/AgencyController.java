package com.rental.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.rental.backend.model.Agency;
import com.rental.backend.dto.AgencyCreationResponse;
import com.rental.backend.repository.AgencyRepository;
import com.rental.backend.repository.BookingRepository;
import com.rental.backend.service.AgencyService;

@RestController
@RequestMapping("/api/agencies")
@CrossOrigin(origins = "http://localhost:3000")
public class AgencyController {
    @Autowired private AgencyService agencyService;
    @Autowired private AgencyRepository agencyRepository;
    @Autowired private BookingRepository bookingRepository;

    @GetMapping
    public List<Agency> getAll() { 
        return agencyService.getAllAgencies(); 
    }

    @GetMapping("/{id}")
    public Agency getOne(@PathVariable Long id) {
        return agencyService.getAgencyById(id).orElse(null);
    }

    @GetMapping("/user/{userId}")
    public Agency getByUser(@PathVariable Long userId) {
        return agencyService.getAgencyByUserId(userId).orElse(null);
    }

    /**
     * Création d'une nouvelle agence
     * Génère automatiquement un compte utilisateur (Role.AGENCY) avec identifiants
     * 
     * @return AgencyCreationResponse contenant l'agence + email + mot de passe généré
     */
    @PostMapping
    public ResponseEntity<AgencyCreationResponse> create(@RequestBody Agency agency) {
        try {
            AgencyCreationResponse response = agencyService.createAgencyWithCredentials(agency);
            System.out.println("✅ Nouvelle agence créée: " + response.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ Erreur création agence: " + e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * Permet à un utilisateur existant de devenir une agence
     */
    @PostMapping("/{userId}/become-agency")
    public ResponseEntity<?> becomeAgency(@PathVariable Long userId, @RequestBody Agency agency) {
        try {
            Agency newAgency = agencyService.createAgencyForUser(userId, agency);
            return ResponseEntity.ok(newAgency);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Agency agencyData) {
        Agency existing = agencyRepository.findById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Mise à jour des champs (sans modifier userId)
        if (agencyData.getName() != null) existing.setName(agencyData.getName());
        if (agencyData.getCity() != null) existing.setCity(agencyData.getCity());
        if (agencyData.getLocation() != null) existing.setLocation(agencyData.getLocation());
        if (agencyData.getDescription() != null) existing.setDescription(agencyData.getDescription());
        if (agencyData.getPhone() != null) existing.setPhone(agencyData.getPhone());
        if (agencyData.getEmail() != null) existing.setEmail(agencyData.getEmail());
        if (agencyData.getWebsite() != null) existing.setWebsite(agencyData.getWebsite());
        if (agencyData.getOpeningHours() != null) existing.setOpeningHours(agencyData.getOpeningHours());
        existing.setOpen(agencyData.isOpen());
        
        Agency saved = agencyService.saveAgency(existing);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!agencyRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        agencyService.deleteAgency(id);
        return ResponseEntity.ok(Map.of("message", "Agence et compte associé supprimés avec succès"));
    }

    // ==================== REVENUS AGENCE ====================
    
    /**
     * Revenus d'une agence spécifique par ID agence
     */
    @GetMapping("/{id}/revenue")
    public ResponseEntity<?> getAgencyRevenue(@PathVariable Long id) {
        if (!agencyRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> revenue = new java.util.HashMap<>();
        
        Double totalRevenue = bookingRepository.getRevenueByAgencyId(id);
        revenue.put("revenue", totalRevenue != null ? totalRevenue : 0.0);
        
        Long totalBookings = bookingRepository.countByAgencyId(id);
        revenue.put("totalBookings", totalBookings);
        
        Long completedBookings = bookingRepository.countCompletedByAgencyId(id);
        revenue.put("completedBookings", completedBookings);
        
        return ResponseEntity.ok(revenue);
    }

    /**
     * Revenus de l'agence liée à un userId (pour le dashboard agence)
     */
    @GetMapping("/user/{userId}/revenue")
    public ResponseEntity<?> getAgencyRevenueByUser(@PathVariable Long userId) {
        return agencyRepository.findByUserId(userId)
            .map(agency -> {
                Map<String, Object> revenue = new java.util.HashMap<>();
                
                Double totalRevenue = bookingRepository.getRevenueByAgencyId(agency.getId());
                revenue.put("revenue", totalRevenue != null ? totalRevenue : 0.0);
                
                Long totalBookings = bookingRepository.countByAgencyId(agency.getId());
                revenue.put("totalBookings", totalBookings);
                
                Long completedBookings = bookingRepository.countCompletedByAgencyId(agency.getId());
                revenue.put("completedBookings", completedBookings);
                
                return ResponseEntity.ok((Object) revenue);
            })
            .orElse(ResponseEntity.notFound().build());
    }
}