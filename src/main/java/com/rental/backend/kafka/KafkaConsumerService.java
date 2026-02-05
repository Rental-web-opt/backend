package com.rental.backend.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import com.rental.backend.controller.NotificationController;
import com.rental.backend.model.NotificationType;
import com.rental.backend.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class KafkaConsumerService {

    @Autowired
    private NotificationController notificationController;

    @Autowired
    private NotificationService notificationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Consomme les événements de réservation et les broadcast
     */
    @KafkaListener(topics = "booking-events", groupId = "easyrent-group")
    public void consumeBookingEvent(String message) {
        System.out.println("📥 Kafka: Booking event received - " + message);
        // Broadcast à tous les utilisateurs connectés
        notificationController.broadcastNotification("booking", message);
    }

    /**
     * Consomme les notifications individuelles ET les persiste en base de données
     * Format: userId|jsonData
     */
    @KafkaListener(topics = "notifications", groupId = "easyrent-group")
    public void consumeNotification(String message) {
        System.out.println("📥 Kafka: Notification received - " + message);
        // Format: userId|jsonData
        String[] parts = message.split("\\|", 2);
        if (parts.length == 2) {
            try {
                Long userId = Long.parseLong(parts[0]);
                String jsonData = parts[1];
                
                // Extraire les informations du JSON
                String type = "INFO";
                String title = "Notification";
                String notificationMessage = jsonData;
                
                try {
                    JsonNode node = objectMapper.readTree(jsonData);
                    if (node.has("type")) {
                        type = node.get("type").asText();
                    }
                    if (node.has("title")) {
                        title = node.get("title").asText();
                    }
                    if (node.has("message")) {
                        notificationMessage = node.get("message").asText();
                    }
                } catch (Exception e) {
                    // Utiliser les valeurs par défaut
                }
                
                // Convertir le type en NotificationType
                NotificationType notificationType = parseNotificationType(type);
                
                // === PERSISTANCE EN BASE DE DONNÉES ===
                notificationService.createNotification(userId, title, notificationMessage, notificationType, jsonData);
                
                // === ENVOI EN TEMPS RÉEL VIA SSE ===
                notificationController.sendNotification(userId, type.toLowerCase(), jsonData);
                
                System.out.println("💾 Notification persistée et envoyée à l'utilisateur " + userId);
                
            } catch (NumberFormatException e) {
                System.out.println("❌ Invalid userId in notification: " + parts[0]);
            }
        }
    }

    /**
     * Consomme les événements à broadcast
     * Format: JSON avec title, message, excludeUserId
     */
    @KafkaListener(topics = "events", groupId = "easyrent-group")
    public void consumeEvent(String message) {
        System.out.println("📥 Kafka: Event received - " + message);
        try {
            JsonNode node = objectMapper.readTree(message);
            Long excludeUserId = null;
            
            if (node.has("excludeUserId") && !node.get("excludeUserId").isNull()) {
                excludeUserId = node.get("excludeUserId").asLong();
            }
            
            // Broadcast à tous les utilisateurs sauf celui exclu
            notificationController.broadcastNotificationExcluding("event", message, excludeUserId);
            
            // Note: Les événements broadcast ne sont pas persistés individuellement
            // car ils sont temporaires (promotions, annonces, etc.)
            
        } catch (Exception e) {
            System.err.println("❌ Erreur parsing event: " + e.getMessage());
            // Fallback: broadcast à tous
            notificationController.broadcastNotification("event", message);
        }
    }

    /**
     * Convertit une chaîne en NotificationType
     */
    private NotificationType parseNotificationType(String type) {
        if (type == null) return NotificationType.INFO;
        
        String upperType = type.toUpperCase();
        
        // Mapper les types de booking vers les types de notification
        if (upperType.contains("BOOKING")) {
            return NotificationType.BOOKING;
        } else if (upperType.contains("PAYMENT")) {
            return NotificationType.PAYMENT;
        } else if (upperType.contains("SUCCESS") || upperType.contains("CONFIRMED") || upperType.contains("APPROVED")) {
            return NotificationType.SUCCESS;
        } else if (upperType.contains("ERROR") || upperType.contains("CANCELLED") || upperType.contains("REJECTED")) {
            return NotificationType.ERROR;
        } else if (upperType.contains("PROMO") || upperType.contains("EVENT")) {
            return NotificationType.PROMO;
        } else if (upperType.contains("SYSTEM")) {
            return NotificationType.SYSTEM;
        }
        
        // Essayer de parser directement
        try {
            return NotificationType.valueOf(upperType);
        } catch (Exception e) {
            return NotificationType.INFO;
        }
    }
}
