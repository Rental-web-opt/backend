package com.rental.backend.service;

import com.rental.backend.model.Notification;
import com.rental.backend.model.NotificationType;
import com.rental.backend.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * Créer une nouvelle notification et la persister en base de données
     */
    @Transactional
    public Notification createNotification(Long userId, String title, String message, NotificationType type) {
        Notification notification = new Notification(userId, title, message, type);
        notification = notificationRepository.save(notification);
        System.out.println("💾 Notification persistée pour user " + userId + ": " + title);
        return notification;
    }

    /**
     * Créer une notification avec des données additionnelles
     */
    @Transactional
    public Notification createNotification(Long userId, String title, String message, NotificationType type, String jsonData) {
        Notification notification = new Notification(userId, title, message, type);
        notification.setData(jsonData);
        notification = notificationRepository.save(notification);
        System.out.println("💾 Notification persistée pour user " + userId + ": " + title);
        return notification;
    }

    /**
     * Récupérer toutes les notifications d'un utilisateur
     */
    public List<Notification> getNotificationsByUserId(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Récupérer les notifications non lues d'un utilisateur
     */
    public List<Notification> getUnreadNotificationsByUserId(Long userId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
    }

    /**
     * Compter les notifications non lues
     */
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    /**
     * Marquer une notification comme lue
     */
    @Transactional
    public Optional<Notification> markAsRead(Long notificationId) {
        Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
        if (notificationOpt.isPresent()) {
            Notification notification = notificationOpt.get();
            notification.setRead(true);
            notificationRepository.save(notification);
            return Optional.of(notification);
        }
        return Optional.empty();
    }

    /**
     * Marquer toutes les notifications d'un utilisateur comme lues
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
        System.out.println("✅ Toutes les notifications marquées comme lues pour user " + userId);
    }

    /**
     * Supprimer une notification
     */
    @Transactional
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    /**
     * Supprimer toutes les notifications d'un utilisateur
     */
    @Transactional
    public void deleteAllByUserId(Long userId) {
        notificationRepository.deleteByUserId(userId);
    }

    /**
     * Méthode utilitaire pour créer des notifications de réservation
     */
    public Notification createBookingNotification(Long userId, String message) {
        return createNotification(userId, "Réservation", message, NotificationType.BOOKING);
    }

    /**
     * Méthode utilitaire pour créer des notifications de paiement
     */
    public Notification createPaymentNotification(Long userId, String message) {
        return createNotification(userId, "Paiement", message, NotificationType.PAYMENT);
    }

    /**
     * Méthode utilitaire pour créer des notifications système
     */
    public Notification createSystemNotification(Long userId, String title, String message) {
        return createNotification(userId, title, message, NotificationType.SYSTEM);
    }
}
