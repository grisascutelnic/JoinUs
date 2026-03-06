package com.scutelnic.joinus.dto.notification;

import com.scutelnic.joinus.entity.NotificationType;

import java.time.Instant;

public record NotificationItemResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        String link,
        boolean read,
        Instant createdAt
) {
}
