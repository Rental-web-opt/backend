package com.rental.backend.model;

public enum EventRequestStatus {
    PENDING,   // En attente d'approbation par l'admin
    APPROVED,  // Approuvé et publié
    REJECTED   // Refusé par l'admin
}
