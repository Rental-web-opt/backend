package com.rental.backend.dto;

import com.rental.backend.model.Agency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de réponse pour la création d'une agence
 * Contient l'agence créée + les identifiants de connexion générés
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgencyCreationResponse {
    private Agency agency;
    private String email;           // Email de connexion généré
    private String generatedPassword; // Mot de passe généré (affiché une seule fois)
    private String message;
    
    public AgencyCreationResponse(Agency agency, String email, String password) {
        this.agency = agency;
        this.email = email;
        this.generatedPassword = password;
        this.message = "Agence créée avec succès. Conservez ces identifiants !";
    }
}
