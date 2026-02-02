package com.rental.backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.rental.backend.model.Driver;
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

    @PostMapping
    public Driver createDriver(@RequestBody Driver driver) {
        return driverService.saveDriver(driver);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Driver> updateDriver(@PathVariable Long id, @RequestBody Driver driverData) {
        Driver existing = driverRepository.findById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (driverData.getFullName() != null) existing.setFullName(driverData.getFullName());
        if (driverData.getEmail() != null) existing.setEmail(driverData.getEmail());
        if (driverData.getPhone() != null) existing.setPhone(driverData.getPhone());
        if (driverData.getLicenseNumber() != null) existing.setLicenseNumber(driverData.getLicenseNumber());
        if (driverData.getExperience() != null) existing.setExperience(driverData.getExperience());
        existing.setAvailable(driverData.isAvailable());
        
        Driver saved = driverRepository.save(existing);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDriver(@PathVariable Long id) {
        driverService.deleteDriver(id);
        return ResponseEntity.noContent().build();
    }
}