package com.rental.backend.repository;

import com.rental.backend.model.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {
    
    // CORRECTION : On utilise 'PricePerDay' car c'est le nom du champ dans l'entité Car
    List<Car> findByPricePerDayLessThanEqual(Double price);
    
    // Si vous avez d'autres méthodes comme findByLocation, laissez-les.
    List<Car> findByLocationContainingIgnoreCase(String location);
}