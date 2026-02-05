package com.rental.backend.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.HashMap;

@Service
public class KafkaProducerService {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== NOTIFICATIONS RÉSERVATION ====================

    /**
     * Notifier l'utilisateur que sa réservation est confirmée
     */
    public void notifyBookingConfirmed(Long userId, String vehicleName, String startDate, String endDate, Double price) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "BOOKING_CONFIRMED");
        data.put("title", "🎉 Réservation confirmée !");
        data.put("message", String.format(
            "Votre réservation du véhicule %s du %s au %s est confirmée. Montant: %,.0f CFA",
            vehicleName, startDate, endDate, price
        ));
        data.put("vehicleName", vehicleName);
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("price", price);
        
        sendNotificationToUser(userId, data);
        System.out.println("🔔 Notification CONFIRMED envoyée à l'utilisateur " + userId);
    }

    /**
     * Notifier l'utilisateur que sa réservation est terminée
     */
    public void notifyBookingCompleted(Long userId, String vehicleName) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "BOOKING_COMPLETED");
        data.put("title", "✅ Réservation terminée");
        data.put("message", String.format(
            "Votre location du véhicule %s est terminée. Merci d'avoir choisi Easy-Rent ! N'hésitez pas à laisser un avis.",
            vehicleName
        ));
        data.put("vehicleName", vehicleName);
        
        sendNotificationToUser(userId, data);
        System.out.println("🔔 Notification COMPLETED envoyée à l'utilisateur " + userId);
    }

    /**
     * Notifier l'utilisateur que sa réservation a été annulée
     */
    public void notifyBookingCancelled(Long userId, String vehicleName) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "BOOKING_CANCELLED");
        data.put("title", "❌ Réservation annulée");
        data.put("message", String.format(
            "Votre réservation du véhicule %s a été annulée avec succès.",
            vehicleName
        ));
        data.put("vehicleName", vehicleName);
        
        sendNotificationToUser(userId, data);
        System.out.println("🔔 Notification CANCELLED envoyée à l'utilisateur " + userId);
    }

    // ==================== NOTIFICATIONS ADMIN/AGENCE ====================

    /**
     * Notifier l'admin d'une nouvelle réservation
     */
    public void notifyAdminNewBooking(Long adminId, String userName, String vehicleName, String agencyName, Double price) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "NEW_BOOKING");
        data.put("title", "📦 Nouvelle réservation");
        data.put("message", String.format(
            "Nouvelle réservation: %s a réservé %s (%s) pour %,.0f CFA",
            userName, vehicleName, agencyName, price
        ));
        data.put("userName", userName);
        data.put("vehicleName", vehicleName);
        data.put("agencyName", agencyName);
        data.put("price", price);
        
        sendNotificationToUser(adminId, data);
        System.out.println("🔔 Notification NEW_BOOKING envoyée à l'admin " + adminId);
    }

    /**
     * Notifier l'agence d'une nouvelle réservation sur l'un de ses véhicules
     */
    public void notifyAgencyNewBooking(Long agencyUserId, String userName, String vehicleName, String startDate, String endDate, Double price) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "NEW_BOOKING");
        data.put("title", "🚗 Nouvelle réservation sur votre véhicule");
        data.put("message", String.format(
            "%s a réservé votre véhicule %s du %s au %s. Montant: %,.0f CFA",
            userName, vehicleName, startDate, endDate, price
        ));
        data.put("userName", userName);
        data.put("vehicleName", vehicleName);
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("price", price);
        
        sendNotificationToUser(agencyUserId, data);
        System.out.println("🔔 Notification NEW_BOOKING envoyée à l'agence (userId: " + agencyUserId + ")");
    }

    // ==================== ÉVÉNEMENTS ====================

    /**
     * Publier un événement à tous les utilisateurs (sauf l'émetteur)
     */
    public void broadcastEvent(String title, String message, Long excludeUserId) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "EVENT");
        data.put("title", "🎯 " + title);
        data.put("message", message);
        data.put("excludeUserId", excludeUserId);
        
        try {
            String payload = objectMapper.writeValueAsString(data);
            kafkaTemplate.send("events", payload);
            System.out.println("📤 Kafka: Événement broadcast envoyé - " + title);
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'envoi de l'événement: " + e.getMessage());
        }
    }

    /**
     * Notifier l'admin d'une demande de publication d'événement par une agence
     */
    public void notifyAdminEventRequest(Long adminId, String agencyName, String eventTitle, String eventMessage, Long eventRequestId) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "EVENT_REQUEST");
        data.put("title", "📝 Demande de publication d'événement");
        data.put("message", String.format(
            "L'agence %s demande à publier: \"%s\"",
            agencyName, eventTitle
        ));
        data.put("agencyName", agencyName);
        data.put("eventTitle", eventTitle);
        data.put("eventMessage", eventMessage);
        data.put("eventRequestId", eventRequestId);
        
        sendNotificationToUser(adminId, data);
        System.out.println("🔔 Demande d'événement envoyée à l'admin " + adminId);
    }

    /**
     * Notifier l'agence que sa demande d'événement a été approuvée
     */
    public void notifyAgencyEventApproved(Long agencyUserId, String eventTitle) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "EVENT_APPROVED");
        data.put("title", "✅ Événement approuvé");
        data.put("message", String.format(
            "Votre événement \"%s\" a été approuvé et publié à tous les utilisateurs.",
            eventTitle
        ));
        
        sendNotificationToUser(agencyUserId, data);
        System.out.println("🔔 Notification EVENT_APPROVED envoyée à l'agence (userId: " + agencyUserId + ")");
    }

    /**
     * Notifier l'agence que sa demande d'événement a été refusée
     */
    public void notifyAgencyEventRejected(Long agencyUserId, String eventTitle, String reason) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "EVENT_REJECTED");
        data.put("title", "❌ Événement refusé");
        data.put("message", String.format(
            "Votre événement \"%s\" a été refusé. Raison: %s",
            eventTitle, reason != null ? reason : "Non spécifiée"
        ));
        
        sendNotificationToUser(agencyUserId, data);
        System.out.println("🔔 Notification EVENT_REJECTED envoyée à l'agence (userId: " + agencyUserId + ")");
    }

    // ==================== MÉTHODES DE BASE ====================

    /**
     * Envoie une notification à un utilisateur spécifique via Kafka
     */
    private void sendNotificationToUser(Long userId, Map<String, Object> data) {
        try {
            String jsonData = objectMapper.writeValueAsString(data);
            String payload = userId + "|" + jsonData;
            kafkaTemplate.send("notifications", payload);
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'envoi de la notification: " + e.getMessage());
        }
    }

    /**
     * Méthode générique pour envoyer un booking event
     */
    public void sendBookingEvent(String message) {
        kafkaTemplate.send("booking-events", message);
        System.out.println("📤 Kafka: Booking event sent - " + message);
    }

    /**
     * Méthode générique pour envoyer une notification
     */
    public void sendNotification(String userId, String message) {
        String payload = userId + "|" + message;
        kafkaTemplate.send("notifications", payload);
        System.out.println("📤 Kafka: Notification sent to user " + userId);
    }
}
