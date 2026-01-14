package com.rental.backend.controller;

import com.rental.backend.model.Role;
import com.rental.backend.model.User;
import com.rental.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@CrossOrigin(origins = "http://localhost:3000")
public class DriverController {

    @Autowired
    private UserRepository userRepository;

    // GET : Tous les chauffeurs (C'est-à-dire les Users avec le rôle DRIVER)
    @GetMapping
    public List<User> getAllDrivers() {
        // CORRECTION : On retourne bien la liste des Users filtrée par Rôle
        return userRepository.findByRole(Role.DRIVER);
    }

    // GET : Un chauffeur par ID
    @GetMapping("/{id}")
    public ResponseEntity<User> getDriverById(@PathVariable Long id) {
        return userRepository.findById(id)
                // On vérifie au passage que cet utilisateur est bien un chauffeur
                .filter(user -> user.getRole() == Role.DRIVER)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}