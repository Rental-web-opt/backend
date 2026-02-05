package com.rental.backend.controller;

import com.rental.backend.model.Notification;
import com.rental.backend.model.NotificationType;
import com.rental.backend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"}, allowCredentials = "true")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    // Stockage des connexions SSE par userId
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    // ============================================
    // 📡 ENDPOINTS SSE (TEMPS RÉEL)
    // ============================================

    /**
     * Endpoint SSE pour recevoir les notifications en temps réel
     */
    @GetMapping(value = "/stream/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable Long userId) {
        System.out.println("🔌 Nouvelle tentative d'abonnement SSE pour l'utilisateur ID: " + userId);
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        
        emitters.put(userId, emitter);
        
        emitter.onCompletion(() -> {
            System.out.println("💨 Connexion SSE terminée (Completion) pour l'utilisateur: " + userId);
            emitters.remove(userId);
        });
        emitter.onTimeout(() -> {
            System.out.println("⏰ Connexion SSE expirée (Timeout) pour l'utilisateur: " + userId);
            emitters.remove(userId);
        });
        emitter.onError((e) -> {
            System.out.println("❌ Erreur SSE pour l'utilisateur: " + userId + " - " + e.getMessage());
            emitters.remove(userId);
        });
        
        // Message de bienvenue
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("Connecté aux notifications en temps réel"));
            System.out.println("✅ Abonnement SSE réussi pour l'utilisateur ID: " + userId);
        } catch (IOException e) {
            System.out.println("❌ Échec de l'envoi du message de bienvenue SSE pour: " + userId);
            emitter.complete();
        }
        
        return emitter;
    }

    // ============================================
    // 💾 ENDPOINTS PERSISTANCE (BASE DE DONNÉES)
    // ============================================

    /**
     * Récupérer toutes les notifications d'un utilisateur
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getNotificationsByUser(@PathVariable Long userId) {
        List<Notification> notifications = notificationService.getNotificationsByUserId(userId);
        System.out.println("📋 " + notifications.size() + " notifications récupérées pour user " + userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Récupérer le nombre de notifications non lues
     */
    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable Long userId) {
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Marquer une notification comme lue
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        return notificationService.markAsRead(id)
                .map(notification -> ResponseEntity.ok(notification))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Marquer toutes les notifications d'un utilisateur comme lues
     */
    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<Map<String, Boolean>> markAllAsRead(@PathVariable Long userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * Créer une nouvelle notification (et l'envoyer via SSE si l'utilisateur est connecté)
     */
    @PostMapping
    public ResponseEntity<Notification> createNotification(@RequestBody CreateNotificationRequest request) {
        NotificationType type;
        try {
            type = NotificationType.valueOf(request.type.toUpperCase());
        } catch (Exception e) {
            type = NotificationType.INFO;
        }

        // Persister en base de données
        Notification notification = notificationService.createNotification(
                request.userId,
                request.title,
                request.message,
                type
        );

        // Envoyer en temps réel via SSE si l'utilisateur est connecté
        sendNotification(request.userId, request.type.toLowerCase(), request.message);

        return ResponseEntity.ok(notification);
    }

    /**
     * Supprimer une notification
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Boolean>> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ============================================
    // 🚀 MÉTHODES SSE INTERNES
    // ============================================

    /**
     * Envoyer une notification SSE à un utilisateur spécifique
     * ET la persister en base de données
     */
    public void sendNotificationAndPersist(Long userId, String title, String message, NotificationType type) {
        // 1. Persister en base de données
        notificationService.createNotification(userId, title, message, type);

        // 2. Envoyer via SSE si l'utilisateur est connecté
        sendNotification(userId, type.name().toLowerCase(), message);
    }

    /**
     * Envoyer une notification SSE à un utilisateur spécifique (sans persistance)
     */
    public void sendNotification(Long userId, String type, String message) {
        System.out.println("🎯 Tentative d'envoi de notification SSE à l'utilisateur " + userId + " (" + type + ")");
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name(type)
                        .data(message));
                System.out.println("🚀 Notification envoyée avec succès via SSE à l'utilisateur: " + userId);
            } catch (IOException e) {
                System.out.println("❌ Erreur lors de l'envoi SSE à l'utilisateur " + userId + ". Suppression de l'émetteur.");
                emitters.remove(userId);
            }
        } else {
            System.out.println("⚠️ Aucun émetteur SSE actif trouvé pour l'utilisateur: " + userId);
        }
    }

    /**
     * Envoyer une notification à tous les utilisateurs connectés ET la persister pour chacun
     */
    public void broadcastNotificationAndPersist(String title, String message, NotificationType type, List<Long> userIds) {
        for (Long userId : userIds) {
            sendNotificationAndPersist(userId, title, message, type);
        }
    }

    /**
     * Envoyer une notification SSE à tous les utilisateurs connectés (sans persistance)
     */
    public void broadcastNotification(String type, String message) {
        System.out.println("📢 Broadcast d'une notification à tous les utilisateurs (" + type + ")");
        int sent = 0;
        int failed = 0;
        
        for (Map.Entry<Long, SseEmitter> entry : emitters.entrySet()) {
            try {
                entry.getValue().send(SseEmitter.event()
                        .name(type)
                        .data(message));
                sent++;
            } catch (IOException e) {
                emitters.remove(entry.getKey());
                failed++;
            }
        }
        
        System.out.println("📢 Broadcast terminé: " + sent + " envoyé(s), " + failed + " échec(s)");
    }

    /**
     * Envoyer une notification à tous les utilisateurs connectés SAUF un
     */
    public void broadcastNotificationExcluding(String type, String message, Long excludeUserId) {
        System.out.println("📢 Broadcast d'une notification (sauf userId: " + excludeUserId + ")");
        int sent = 0;
        int failed = 0;
        
        for (Map.Entry<Long, SseEmitter> entry : emitters.entrySet()) {
            // Exclure l'utilisateur spécifié
            if (excludeUserId != null && entry.getKey().equals(excludeUserId)) {
                continue;
            }
            
            try {
                entry.getValue().send(SseEmitter.event()
                        .name(type)
                        .data(message));
                sent++;
            } catch (IOException e) {
                emitters.remove(entry.getKey());
                failed++;
            }
        }
        
        System.out.println("📢 Broadcast terminé: " + sent + " envoyé(s), " + failed + " échec(s), 1 exclu");
    }

    // ============================================
    // 🧪 ENDPOINTS DE TEST
    // ============================================

    /**
     * Endpoint pour tester l'envoi de notifications
     */
    @PostMapping("/send/{userId}")
    public String sendTestNotification(
            @PathVariable Long userId,
            @RequestParam String message) {
        // Persister et envoyer
        sendNotificationAndPersist(userId, "Test", message, NotificationType.INFO);
        return "Notification envoyée et persistée pour l'utilisateur " + userId;
    }

    /**
     * Endpoint pour obtenir le nombre d'utilisateurs connectés via SSE
     */
    @GetMapping("/connected-count")
    public Map<String, Integer> getConnectedCount() {
        return Map.of("count", emitters.size());
    }

    /**
     * Endpoint pour tester un broadcast
     */
    @PostMapping("/broadcast")
    public String broadcastTest(@RequestParam String message) {
        broadcastNotification("broadcast", message);
        return "Broadcast envoyé à " + emitters.size() + " utilisateur(s)";
    }

    // ============================================
    // 📦 DTO INTERNE
    // ============================================

    public static class CreateNotificationRequest {
        public Long userId;
        public String title;
        public String message;
        public String type;
    }
}
