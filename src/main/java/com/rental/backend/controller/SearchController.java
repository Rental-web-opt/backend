package com.rental.backend.controller;

import com.rental.backend.elasticsearch.CarDocument;
import com.rental.backend.elasticsearch.CarSearchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired(required = false)
    private CarSearchRepository carSearchRepository;

    @Autowired(required = false)
    private com.rental.backend.elasticsearch.AgencySearchRepository agencySearchRepository;

    @GetMapping("/cars")
    public List<CarDocument> searchCars(@RequestParam String query) {
        if (carSearchRepository == null) {
            return Collections.emptyList();
        }
        return carSearchRepository.findByNameContainingOrBrandContaining(query, query);
    }

    @GetMapping("/agencies")
    public List<com.rental.backend.elasticsearch.AgencyDocument> searchAgencies(@RequestParam String query) {
        if (agencySearchRepository == null) {
            return Collections.emptyList();
        }
        return agencySearchRepository.findByNameContaining(query);
    }

    @GetMapping("/agencies/city/{city}")
    public List<com.rental.backend.elasticsearch.AgencyDocument> searchAgenciesByCity(@PathVariable String city) {
        if (agencySearchRepository == null) {
            return Collections.emptyList();
        }
        return agencySearchRepository.findByCity(city);
    }

    @GetMapping("/cars/type/{type}")
    public List<CarDocument> searchByType(@PathVariable String type) {
        if (carSearchRepository == null) {
            return Collections.emptyList();
        }
        return carSearchRepository.findByType(type);
    }

    @GetMapping("/cars/available")
    public List<CarDocument> getAvailableCars() {
        if (carSearchRepository == null) {
            return Collections.emptyList();
        }
        return carSearchRepository.findByAvailableTrue();
    }

    @GetMapping("/cars/price")
    public List<CarDocument> searchByPriceRange(
            @RequestParam Double min,
            @RequestParam Double max) {
        if (carSearchRepository == null) {
            return Collections.emptyList();
        }
        return carSearchRepository.findByPricePerDayBetween(min, max);
    }
}
