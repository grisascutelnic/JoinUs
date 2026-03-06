package com.scutelnic.joinus.service;

import com.scutelnic.joinus.dto.notification.NotificationItemResponse;
import com.scutelnic.joinus.dto.notification.NotificationsResponse;
import com.scutelnic.joinus.entity.Notification;
import com.scutelnic.joinus.entity.NotificationType;
import com.scutelnic.joinus.entity.User;
import com.scutelnic.joinus.repository.NotificationRepository;
import com.scutelnic.joinus.repository.UserRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class NotificationService {

    private static final int MAX_LIMIT = 50;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public NotificationsResponse getForUser(String email, int limit) {
        User user = requireUser(email);
        int safeLimit = Math.max(1, Math.min(limit, MAX_LIMIT));

        List<NotificationItemResponse> items = notificationRepository
                .findByRecipientOrderByCreatedAtDesc(user, PageRequest.of(0, safeLimit))
                .stream()
                .map(this::toResponse)
                .toList();

        long unreadCount = notificationRepository.countByRecipientAndIsReadFalse(user);
        return new NotificationsResponse(unreadCount, items);
    }

    @Transactional
    public void markAsRead(String email, Long notificationId) {
        User user = requireUser(email);
        Notification notification = notificationRepository.findByIdAndRecipient(notificationId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (!notification.isRead()) {
            notification.setRead(true);
        }
    }

    @Transactional
    public void markAllAsRead(String email) {
        User user = requireUser(email);
        notificationRepository.markAllAsReadByRecipient(user);
    }

    @Transactional
    public void createWelcomeNotification(User user) {
        if (notificationRepository.existsByRecipientAndType(user, NotificationType.WELCOME)) {
            return;
        }

        Notification notification = new Notification();
        notification.setRecipient(user);
        notification.setType(NotificationType.WELCOME);
        notification.setTitle("Bun venit in JoinUs!");
        notification.setMessage("Contul tau a fost creat cu succes. Completeaza-ti profilul si descopera activitati noi.");
        notification.setLink("/profile/edit?completeProfile");
        notificationRepository.save(notification);
    }

    @Transactional
    public void sendAdminNotificationToUser(Long recipientUserId,
                                            String title,
                                            String message,
                                            String link) {
        User recipient = userRepository.findById(recipientUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        createAdminNotification(recipient, title, message, link);
    }

    @Transactional
    public int sendAdminNotificationToAll(String title,
                                          String message,
                                          String link) {
        List<User> users = userRepository.findAll();
        users.forEach(user -> createAdminNotification(user, title, message, link));
        return users.size();
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void backfillWelcomeNotificationsForExistingUsers() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            createWelcomeNotification(user);
        }
    }

    private User requireUser(String email) {
        String normalized = email == null ? "" : email.toLowerCase().trim();
        return userRepository.findByEmail(normalized)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));
    }

    private NotificationItemResponse toResponse(Notification notification) {
        return new NotificationItemResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getLink(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }

    private void createAdminNotification(User recipient,
                                         String title,
                                         String message,
                                         String link) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(NotificationType.ADMIN);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setLink(link);
        notificationRepository.save(notification);
    }
}
