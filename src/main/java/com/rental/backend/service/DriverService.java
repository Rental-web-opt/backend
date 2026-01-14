package com.rental.backend.service;

import com.rental.backend.model.Driver;
import com.rental.backend.repository.DriverRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class DriverService {
    @Autowired private DriverRepository driverRepository;

    public List<Driver> getAllDrivers() { return driverRepository.findAll(); }
    public Optional<Driver> getDriverById(Long id) { return driverRepository.findById(id); }
    public Driver saveDriver(Driver driver) { return driverRepository.save(driver); }
    public void deleteDriver(Long id) { driverRepository.deleteById(id); }
    
    public List<Driver> getDriversByLocation(String location) {
        return driverRepository.findByLocationContainingIgnoreCase(location);
    }
}