package com.rental.backend.controller;

import com.rental.backend.model.Car;
import com.rental.backend.service.CarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/cars")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"}, allowCredentials = "true")
public class CarController {
    @Autowired private CarService carService;
    @Autowired private com.rental.backend.repository.CarRepository carRepository;

    @GetMapping
    public List<Car> getAll() { return carService.getAllCars(); }

    @GetMapping("/{id}")
    public ResponseEntity<Car> getOne(@PathVariable Long id) {
        return carService.getCarById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/agency/{agencyId}")
    public List<Car> getByAgency(@PathVariable Long agencyId) {
        return carService.getCarsByAgency(agencyId);
    }

    @PostMapping
    public Car create(@RequestBody Car car) { return carService.saveCar(car); }

    /**
     * Modifier un véhicule existant
     * PUT /api/cars/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Car> update(@PathVariable Long id, @RequestBody Car carData) {
        Car existing = carRepository.findById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        if (carData.getName() != null) existing.setName(carData.getName());
        if (carData.getBrand() != null) existing.setBrand(carData.getBrand());
        if (carData.getModel() != null) existing.setModel(carData.getModel());
        if (carData.getType() != null) existing.setType(carData.getType());
        if (carData.getPricePerDay() != null) existing.setPricePerDay(carData.getPricePerDay());
        if (carData.getPricePerHour() != null) existing.setPricePerHour(carData.getPricePerHour());
        if (carData.getMonthlyPrice() != null) existing.setMonthlyPrice(carData.getMonthlyPrice());
        if (carData.getLocation() != null) existing.setLocation(carData.getLocation());
        if (carData.getDescription() != null) existing.setDescription(carData.getDescription());
        if (carData.getImage() != null) existing.setImage(carData.getImage());
        if (carData.getTransmission() != null) existing.setTransmission(carData.getTransmission());
        if (carData.getFuelType() != null) existing.setFuelType(carData.getFuelType());
        if (carData.getSeats() != null) existing.setSeats(carData.getSeats());
        if (carData.getMaxSpeed() != null) existing.setMaxSpeed(carData.getMaxSpeed());
        existing.setAvailable(carData.isAvailable());

        Car saved = carRepository.save(existing);
        System.out.println("✅ Véhicule mis à jour: " + saved.getName());
        return ResponseEntity.ok(saved);
    }
    
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) { carService.deleteCar(id); }
}