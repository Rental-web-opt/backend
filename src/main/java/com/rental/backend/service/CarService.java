package com.rental.backend.service;

import com.rental.backend.model.Car;
import com.rental.backend.repository.CarRepository;
import com.rental.backend.elasticsearch.CarDocument;
import com.rental.backend.elasticsearch.CarSearchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class CarService {
    @Autowired private CarRepository carRepository;
    @Autowired(required = false) private CarSearchRepository carSearchRepository;

    public List<Car> getAllCars() { return carRepository.findAll(); }
    public Optional<Car> getCarById(Long id) { return carRepository.findById(id); }

    public Car saveCar(Car car) {
        Car savedCar = carRepository.save(car);
        
        // Synchronisation avec Elasticsearch
        if (carSearchRepository != null) {
            try {
                CarDocument doc = mapToDocument(savedCar);
                carSearchRepository.save(doc);
                System.out.println("🔄 Indexé dans Elasticsearch : " + doc.getName());
            } catch (Exception e) {
                System.err.println("❌ Erreur d'indexation ES : " + e.getMessage());
            }
        }
        return savedCar;
    }

    public void deleteCar(Long id) {
        carRepository.deleteById(id);
        if (carSearchRepository != null) {
            carSearchRepository.deleteById(id.toString());
        }
    }

    private CarDocument mapToDocument(Car car) {
        CarDocument doc = new CarDocument();
        doc.setId(car.getId().toString());
        doc.setName(car.getName());
        doc.setBrand(car.getBrand());
        doc.setType(car.getType());
        doc.setPricePerDay(car.getPricePerDay());
        doc.setAvailable(car.isAvailable());
        return doc;
    }
}