package com.rental.backend.model;

/**
 * Cible d'une offre/promotion
 */
public enum OfferTarget {
    CLIENTS,     // Tous les clients (utilisateurs normaux)
    AGENCIES,    // Toutes les agences
    DRIVERS,     // Tous les chauffeurs
    ALL          // Tout le monde
}
