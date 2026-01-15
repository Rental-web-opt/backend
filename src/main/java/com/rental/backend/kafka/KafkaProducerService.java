package com.rental.backend.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void sendBookingEvent(String message) {
        kafkaTemplate.send("booking-events", message);
        System.out.println("📤 Kafka: Booking event sent - " + message);
    }

    public void sendNotification(String userId, String message) {
        String payload = userId + "|" + message;
        kafkaTemplate.send("notifications", payload);
        System.out.println("📤 Kafka: Notification sent to user " + userId);
    }
}
