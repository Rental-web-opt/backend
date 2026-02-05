package com.rental.backend.dto;

import com.rental.backend.model.Driver;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de réponse pour la création d'un chauffeur
 * Contient le chauffeur créé + les identifiants de connexion générés
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverCreationResponse {
    private Driver driver;
    private String email;           // Email de connexion généré
    private String generatedPassword; // Mot de passe généré (affiché une seule fois)
    private String message;
    
    public DriverCreationResponse(Driver driver, String email, String password) {
        this.driver = driver;
        this.email = email;
        this.generatedPassword = password;
        this.message = "Chauffeur créé avec succès. Conservez ces identifiants !";
    }
}
