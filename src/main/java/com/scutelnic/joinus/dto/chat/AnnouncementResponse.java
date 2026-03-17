package com.scutelnic.joinus.dto.chat;

import java.time.LocalDateTime;

public record AnnouncementResponse(
        Long id,
        Long activityId,
        Long senderId,
        String senderName,
        String content,
        LocalDateTime createdAt
) {
}
