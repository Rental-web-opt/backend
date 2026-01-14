package com.rental.backend.controller;

import com.rental.backend.model.Car;
import com.rental.backend.service.CarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/cars")
@CrossOrigin(origins = "http://localhost:3000")
public class CarController {
    @Autowired private CarService carService;

    @GetMapping
    public List<Car> getAll() { return carService.getAllCars(); }

    @GetMapping("/{id}")
    public ResponseEntity<Car> getOne(@PathVariable Long id) {
        return carService.getCarById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Car create(@RequestBody Car car) { return carService.saveCar(car); }
    
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) { carService.deleteCar(id); }
}