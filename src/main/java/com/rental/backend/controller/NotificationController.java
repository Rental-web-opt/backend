package com.rental.backend.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class NotificationController {

    // Stockage des connexions SSE par userId
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Endpoint SSE pour recevoir les notifications en temps réel
     */
    @GetMapping(value = "/stream/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable Long userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        
        emitters.put(userId, emitter);
        
        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError((e) -> emitters.remove(userId));
        
        // Message de bienvenue
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("Connecté aux notifications en temps réel"));
        } catch (IOException e) {
            emitter.complete();
        }
        
        return emitter;
    }

    /**
     * Envoyer une notification à un utilisateur spécifique
     */
    public void sendNotification(Long userId, String type, String message) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name(type)
                        .data(message));
            } catch (IOException e) {
                emitters.remove(userId);
            }
        }
    }

    /**
     * Envoyer une notification à tous les utilisateurs connectés
     */
    public void broadcastNotification(String type, String message) {
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(type)
                        .data(message));
            } catch (IOException e) {
                emitters.remove(userId);
            }
        });
    }

    /**
     * Endpoint pour tester l'envoi de notifications
     */
    @PostMapping("/send/{userId}")
    public String sendTestNotification(
            @PathVariable Long userId,
            @RequestParam String message) {
        sendNotification(userId, "notification", message);
        return "Notification envoyée à l'utilisateur " + userId;
    }
}
