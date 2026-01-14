package com.rental.backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rental.backend.model.Agency;
import com.rental.backend.service.AgencyService;

@RestController
@RequestMapping("/api/agencies")
@CrossOrigin(origins = "http://localhost:3000")
public class AgencyController {
    @Autowired private AgencyService agencyService;

    @GetMapping
    public List<Agency> getAll() { return agencyService.getAllAgencies(); }

    @GetMapping("/{id}")
    public Agency getOne(@PathVariable Long id) {
        return agencyService.getAgencyById(id).orElse(null);
    }

    @PostMapping
    public Agency create(@RequestBody Agency agency) { return agencyService.saveAgency(agency); }
}