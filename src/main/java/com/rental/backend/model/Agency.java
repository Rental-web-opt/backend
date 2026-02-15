package com.rental.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "agencies")
public class Agency {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Lien vers le compte utilisateur de l'agence
    private Long userId;

    private String name;
    private String city;
    private String location; // Adresse précise
    
    @Column(length = 2000)
    private String description;

    // Visuels
    private String logo;
    private String coverImage;

    // Contact
    private String phone;
    private String email;
    private String website;

    // ==================== HORAIRES D'OUVERTURE ====================
    
    // Horaires par jour (format JSON pour les horaires structurés)
    @Column(length = 2000)
    private String openingHours; // Peut contenir JSON ou texte simple - gardé pour compatibilité
    
    // Horaires détaillés par jour
    private String mondayHours;    // "08:00-18:00" ou "fermé"
    private String tuesdayHours;   // "08:00-18:00" ou "fermé"
    private String wednesdayHours; // "08:00-18:00" ou "fermé"
    private String thursdayHours;  // "08:00-18:00" ou "fermé"
    private String fridayHours;    // "08:00-18:00" ou "fermé"
    private String saturdayHours;  // "08:00-14:00" ou "fermé"
    private String sundayHours;    // "fermé" ou "09:00-12:00"
    
    // Statut ouvert/fermé (calculé automatiquement ou forcé)
    private boolean isOpen;
    private boolean forceOpen;  // Si true, ignorer le calcul automatique
    private boolean forceClosed; // Si true, forcer fermé (vacances, etc.)
    
    // Social
    private Double rating;
    private Integer reviewCount;

    @ElementCollection
    private List<String> tags; // ["Luxe", "Aéroport", "24/7"]

    // Liste des voitures de l'agence
    @OneToMany(mappedBy = "agency", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Car> vehicles;

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Vérifie si l'agence est actuellement ouverte basé sur les horaires
     */
    @Transient
    public boolean isCurrentlyOpen() {
        // Si forcé fermé (vacances, etc.)
        if (forceClosed) {
            return false;
        }
        
        // Si forcé ouvert (24/7)
        if (forceOpen) {
            return true;
        }
        
        // Vérifier selon le jour et l'heure actuels
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek today = now.getDayOfWeek();
        LocalTime currentTime = now.toLocalTime();
        
        String todayHours = getHoursForDay(today);
        
        if (todayHours == null || todayHours.equalsIgnoreCase("fermé") || todayHours.isEmpty()) {
            return false;
        }
        
        // Parser les horaires (format "08:00-18:00")
        try {
            String[] parts = todayHours.split("-");
            if (parts.length == 2) {
                LocalTime openTime = LocalTime.parse(parts[0].trim());
                LocalTime closeTime = LocalTime.parse(parts[1].trim());
                
                return !currentTime.isBefore(openTime) && currentTime.isBefore(closeTime);
            }
        } catch (Exception e) {
            // En cas d'erreur de parsing, retourner le champ isOpen
        }
        
        return isOpen;
    }

    /**
     * Retourne les horaires pour un jour donné
     */
    public String getHoursForDay(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> mondayHours;
            case TUESDAY -> tuesdayHours;
            case WEDNESDAY -> wednesdayHours;
            case THURSDAY -> thursdayHours;
            case FRIDAY -> fridayHours;
            case SATURDAY -> saturdayHours;
            case SUNDAY -> sundayHours;
        };
    }

    /**
     * Retourne un résumé formaté des horaires pour l'affichage
     */
    @Transient
    public String getFormattedSchedule() {
        StringBuilder sb = new StringBuilder();
        
        if (mondayHours != null) sb.append("Lun: ").append(mondayHours).append(" | ");
        if (tuesdayHours != null) sb.append("Mar: ").append(tuesdayHours).append(" | ");
        if (wednesdayHours != null) sb.append("Mer: ").append(wednesdayHours).append(" | ");
        if (thursdayHours != null) sb.append("Jeu: ").append(thursdayHours).append(" | ");
        if (fridayHours != null) sb.append("Ven: ").append(fridayHours).append(" | ");
        if (saturdayHours != null) sb.append("Sam: ").append(saturdayHours).append(" | ");
        if (sundayHours != null) sb.append("Dim: ").append(sundayHours);
        
        return sb.toString().replaceAll(" \\| $", "");
    }
}