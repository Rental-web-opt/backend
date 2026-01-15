package com.rental.backend.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {

    @KafkaListener(topics = "booking-events", groupId = "easyrent-group")
    public void consumeBookingEvent(String message) {
        System.out.println("📥 Kafka: Booking event received - " + message);
        // Traitement de l'événement de réservation
    }

    @KafkaListener(topics = "notifications", groupId = "easyrent-group")
    public void consumeNotification(String message) {
        System.out.println("📥 Kafka: Notification received - " + message);
        // Traitement de la notification (pourrait être envoyé via SSE/WebSocket)
    }
}
