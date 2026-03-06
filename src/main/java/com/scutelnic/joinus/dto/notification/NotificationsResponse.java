package com.scutelnic.joinus.dto.notification;

import java.util.List;

public record NotificationsResponse(
        long unreadCount,
        List<NotificationItemResponse> notifications
) {
}
