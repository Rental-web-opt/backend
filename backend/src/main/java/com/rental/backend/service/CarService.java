package com.rental.backend.service;

import com.rental.backend.model.Car;
import com.rental.backend.repository.CarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class CarService {
    @Autowired private CarRepository carRepository;

    public List<Car> getAllCars() { return carRepository.findAll(); }
    public Optional<Car> getCarById(Long id) { return carRepository.findById(id); }
    public Car saveCar(Car car) { return carRepository.save(car); }
    public void deleteCar(Long id) { carRepository.deleteById(id); }
    
    // Recherche par mot clé (nom ou marque)
    // Nécessite d'ajouter la méthode dans le Repository
}