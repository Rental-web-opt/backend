package com.rental.backend.controller;

import com.rental.backend.model.User;
import com.rental.backend.model.Role; 
import com.rental.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    // Classe interne pour recevoir les données de login
    static class LoginRequest {
        public String email;
        public String password;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // VÉRIFICATION DU MOT DE PASSE (Simple pour l'instant)
            if (user.getPassword().equals(request.password)) {
                // On renvoie l'utilisateur COMPLET (avec l'ID)
                return ResponseEntity.ok(user);
            }
        }
        
        return ResponseEntity.status(401).body(Map.of("message", "Email ou mot de passe incorrect"));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cet email est déjà utilisé."));
        }
        
        // --- CORRECTION ICI ---
        // On utilise l'Enum Role.USER et non la chaîne "USER"
        if (user.getRole() == null) {
            user.setRole(Role.USER); 
        }
        
        User savedUser = userRepository.save(user);
        return ResponseEntity.ok(savedUser);
    }
}