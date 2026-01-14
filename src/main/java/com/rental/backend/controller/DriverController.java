package com.rental.backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rental.backend.model.Driver;
import com.rental.backend.service.DriverService;

@RestController
@RequestMapping("/api/drivers")
@CrossOrigin(origins = "http://localhost:3000") // Pour le Frontend Next.js
public class DriverController {

    @Autowired
    private DriverService driverService;

    // GET : Tous les chauffeurs
    @GetMapping
    public List<Driver> getAllDrivers() {
        return driverService.getAllDrivers();
    }

    // GET : Un chauffeur par ID
    @GetMapping("/{id}")
    public ResponseEntity<Driver> getDriverById(@PathVariable Long id) {
        return driverService.getDriverById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET : Filtrer par ville (Ex: /api/drivers/search?city=Douala)
    @GetMapping("/search")
    public List<Driver> getDriversByCity(@RequestParam String city) {
        return driverService.getDriversByLocation(city);
    }

    // POST : Créer un chauffeur
    @PostMapping
    public Driver createDriver(@RequestBody Driver driver) {
        return driverService.saveDriver(driver);
    }

    // DELETE : Supprimer un chauffeur
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDriver(@PathVariable Long id) {
        driverService.deleteDriver(id);
        return ResponseEntity.noContent().build();
    }
}