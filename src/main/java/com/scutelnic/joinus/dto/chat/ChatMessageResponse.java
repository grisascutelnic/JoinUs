package com.scutelnic.joinus.dto.chat;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        Long activityId,
        Long senderId,
        String senderName,
        String content,
        LocalDateTime createdAt,
        long deliveredCount,
        long seenCount
) {
}
