package com.rental.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.rental.backend.model.Driver;
import com.rental.backend.dto.DriverCreationResponse;
import com.rental.backend.repository.DriverRepository;
import com.rental.backend.service.DriverService;

@RestController
@RequestMapping("/api/drivers")
@CrossOrigin(origins = "http://localhost:3000")
public class DriverController {

    @Autowired
    private DriverService driverService;
    
    @Autowired
    private DriverRepository driverRepository;

    @GetMapping
    public List<Driver> getAllDrivers() {
        return driverService.getAllDrivers();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Driver> getDriverById(@PathVariable Long id) {
        return driverService.getDriverById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public List<Driver> getDriversByCity(@RequestParam String city) {
        return driverService.getDriversByLocation(city);
    }

    @GetMapping("/available")
    public List<Driver> getAvailableDrivers() {
        return driverRepository.findByAvailable(true);
    }

    /**
     * Création d'un nouveau chauffeur
     * Génère automatiquement un compte utilisateur (Role.DRIVER) avec identifiants
     * 
     * @return DriverCreationResponse contenant le chauffeur + email + mot de passe généré
     */
    @PostMapping
    public ResponseEntity<DriverCreationResponse> createDriver(@RequestBody Driver driver) {
        try {
            DriverCreationResponse response = driverService.createDriverWithCredentials(driver);
            System.out.println("✅ Nouveau chauffeur créé: " + response.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ Erreur création chauffeur: " + e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Driver> updateDriver(@PathVariable Long id, @RequestBody Driver driverData) {
        Driver existing = driverRepository.findById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (driverData.getFullName() != null) existing.setFullName(driverData.getFullName());
        if (driverData.getName() != null) existing.setName(driverData.getName());
        if (driverData.getPhone() != null) existing.setPhone(driverData.getPhone());
        if (driverData.getLicenseNumber() != null) existing.setLicenseNumber(driverData.getLicenseNumber());
        if (driverData.getExperience() != null) existing.setExperience(driverData.getExperience());
        if (driverData.getAge() != null) existing.setAge(driverData.getAge());
        if (driverData.getLocation() != null) existing.setLocation(driverData.getLocation());
        if (driverData.getPricePerDay() != null) existing.setPricePerDay(driverData.getPricePerDay());
        if (driverData.getBio() != null) existing.setBio(driverData.getBio());
        existing.setAvailable(driverData.isAvailable());
        
        Driver saved = driverRepository.save(existing);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDriver(@PathVariable Long id) {
        if (!driverRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        driverService.deleteDriver(id);
        return ResponseEntity.ok(Map.of("message", "Chauffeur et compte associé supprimés avec succès"));
    }
}