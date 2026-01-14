package com.rental.backend.controller;

import com.rental.backend.model.Role;
import com.rental.backend.model.User;
import com.rental.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserRepository userRepository;

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