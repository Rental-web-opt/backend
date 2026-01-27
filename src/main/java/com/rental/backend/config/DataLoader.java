package com.rental.backend.config;

import com.rental.backend.model.*;
import com.rental.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

//@Component
public class DataLoader implements CommandLineRunner {

    @Autowired private UserRepository userRepository;
    @Autowired private AgencyRepository agencyRepository;
    @Autowired private CarRepository carRepository;
    @Autowired private DriverRepository driverRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired(required = false) private com.rental.backend.elasticsearch.CarSearchRepository carSearchRepository;
    @Autowired(required = false) private com.rental.backend.elasticsearch.AgencySearchRepository agencySearchRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            System.out.println("=== Initialisation des données ===");

            // Utilisateurs
            User admin = new User();
            admin.setFullName("Admin");
            admin.setEmail("admin@easyrent.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);

            User user = new User();
            user.setFullName("Jean Dupont");
            user.setEmail("user@easyrent.com");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setRole(Role.USER);
            userRepository.save(user);

            // Agences
            Agency agency1 = new Agency();
            agency1.setName("AutoLux Douala");
            agency1.setCity("Douala");
            agency1.setOpen(true);
            agencyRepository.save(agency1);

            Agency agency2 = new Agency();
            agency2.setName("Premium Cars Yaoundé");
            agency2.setCity("Yaoundé");
            agency2.setOpen(true);
            agencyRepository.save(agency2);

            // Voitures
            Car car1 = new Car();
            car1.setName("Mercedes GLE 450");
            car1.setBrand("Mercedes");
            car1.setType("SUV");
            car1.setPricePerDay(75000.0);
            car1.setAvailable(true);
            car1.setAgency(agency1);
            carRepository.save(car1);

            Car car2 = new Car();
            car2.setName("Toyota Camry");
            car2.setBrand("Toyota");
            car2.setType("Berline");
            car2.setPricePerDay(45000.0);
            car2.setAvailable(true);
            car2.setAgency(agency1);
            carRepository.save(car2);

            Car car3 = new Car();
            car3.setName("Range Rover Evoque");
            car3.setBrand("Land Rover");
            car3.setType("SUV");
            car3.setPricePerDay(120000.0);
            car3.setAvailable(true);
            car3.setAgency(agency2);
            carRepository.save(car3);

            // Chauffeur
            Driver driver = new Driver();
            driver.setName("Paul Biya");
            driver.setAge(45);
            driver.setExperience("15 ans");
            driver.setLocation("Yaoundé");
            driver.setPricePerDay(25000.0);
            driver.setRating(4.8);
            driverRepository.save(driver);

            System.out.println("=== Données initialisées avec succès ===");
            
            // Indexation initiale dans Elasticsearch
            if (carSearchRepository != null) {
                carRepository.findAll().forEach(car -> {
                    com.rental.backend.elasticsearch.CarDocument doc = new com.rental.backend.elasticsearch.CarDocument();
                    doc.setId(car.getId().toString());
                    doc.setName(car.getName());
                    doc.setBrand(car.getBrand());
                    doc.setType(car.getType());
                    doc.setPricePerDay(car.getPricePerDay());
                    doc.setAvailable(car.getAvailable());
                    carSearchRepository.save(doc);
                });
                System.out.println("⚡ Elasticsearch synchronisé avec les voitures.");
            }

            if (agencySearchRepository != null) {
                agencyRepository.findAll().forEach(agency -> {
                    com.rental.backend.elasticsearch.AgencyDocument doc = new com.rental.backend.elasticsearch.AgencyDocument();
                    doc.setId(agency.getId().toString());
                    doc.setName(agency.getName());
                    doc.setCity(agency.getCity());
                    doc.setLocation(agency.getLocation());
                    doc.setIsOpen(agency.isOpen());
                    agencySearchRepository.save(doc);
                });
                System.out.println("⚡ Elasticsearch synchronisé avec les agences.");
            }
            
            System.out.println("Admin: admin@easyrent.com / admin123");
            System.out.println("User: user@easyrent.com / user123");
        }
    }
}
