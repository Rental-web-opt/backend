package com.rental.backend.controller;

import com.rental.backend.model.Role;
import com.rental.backend.model.User;
import com.rental.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"}, allowCredentials = "true")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Modifier le profil utilisateur (nom, email)
     * PUT /api/users/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProfile(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        // Mise à jour du nom
        if (payload.containsKey("fullName") && payload.get("fullName") != null) {
            String newName = payload.get("fullName").trim();
            if (!newName.isEmpty()) {
                user.setFullName(newName);
            }
        }

        // Mise à jour de l'email (vérifier unicité)
        if (payload.containsKey("email") && payload.get("email") != null) {
            String newEmail = payload.get("email").trim().toLowerCase();
            if (!newEmail.isEmpty() && !newEmail.equals(user.getEmail())) {
                if (userRepository.findByEmail(newEmail).isPresent()) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Cet email est déjà utilisé par un autre compte"));
                }
                user.setEmail(newEmail);
            }
        }

        User updatedUser = userRepository.save(user);
        System.out.println("✅ Profil utilisateur mis à jour: " + updatedUser.getFullName() + " (" + updatedUser.getEmail() + ")");
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Changer le mot de passe
     * PUT /api/users/{id}/password
     * Body: { currentPassword, newPassword }
     */
    @PutMapping("/{id}/password")
    public ResponseEntity<?> changePassword(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        String currentPassword = payload.get("currentPassword");
        String newPassword = payload.get("newPassword");

        if (currentPassword == null || newPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Les champs currentPassword et newPassword sont requis"));
        }

        // Vérifier le mot de passe actuel
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("message", "Le mot de passe actuel est incorrect"));
        }

        // Valider le nouveau mot de passe
        if (newPassword.length() < 4) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le nouveau mot de passe doit contenir au moins 4 caractères"));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        System.out.println("✅ Mot de passe modifié pour l'utilisateur: " + user.getEmail());
        return ResponseEntity.ok(Map.of("message", "Mot de passe modifié avec succès"));
    }

    /**
     * Upgrade du rôle utilisateur (USER → DRIVER ou AGENCY)
     * POST /api/users/{id}/upgrade
     */
    @PostMapping("/{id}/upgrade")
    public ResponseEntity<?> upgradeUser(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        String newRole = payload.get("role");
        
        if (newRole != null) {
            if (newRole.equalsIgnoreCase("DRIVER")) {
                user.setRole(Role.DRIVER);
            } else if (newRole.equalsIgnoreCase("AGENCY")) {
                user.setRole(Role.AGENCY);
            }
        }
        
        User updatedUser = userRepository.save(user);
        return ResponseEntity.ok(updatedUser);
    }
}