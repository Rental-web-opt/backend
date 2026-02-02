package com.rental.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.rental.backend.model.Agency;
import com.rental.backend.repository.AgencyRepository;
import com.rental.backend.service.AgencyService;

@RestController
@RequestMapping("/api/agencies")
@CrossOrigin(origins = "http://localhost:3000")
public class AgencyController {
    @Autowired private AgencyService agencyService;
    @Autowired private AgencyRepository agencyRepository;

    @GetMapping
    public List<Agency> getAll() { return agencyService.getAllAgencies(); }

    @GetMapping("/{id}")
    public Agency getOne(@PathVariable Long id) {
        return agencyService.getAgencyById(id).orElse(null);
    }

    @PostMapping
    public Agency create(@RequestBody Agency agency) { 
        return agencyService.saveAgency(agency); 
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Agency agencyData) {
        Agency existing = agencyRepository.findById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Mise à jour des champs
        if (agencyData.getName() != null) existing.setName(agencyData.getName());
        if (agencyData.getCity() != null) existing.setCity(agencyData.getCity());
        if (agencyData.getLocation() != null) existing.setLocation(agencyData.getLocation());
        if (agencyData.getDescription() != null) existing.setDescription(agencyData.getDescription());
        if (agencyData.getPhone() != null) existing.setPhone(agencyData.getPhone());
        if (agencyData.getEmail() != null) existing.setEmail(agencyData.getEmail());
        if (agencyData.getWebsite() != null) existing.setWebsite(agencyData.getWebsite());
        if (agencyData.getOpeningHours() != null) existing.setOpeningHours(agencyData.getOpeningHours());
        existing.setOpen(agencyData.isOpen());
        
        Agency saved = agencyRepository.save(existing);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!agencyRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        agencyRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Agence supprimée avec succès"));
    }
}