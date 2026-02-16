package com.scutelnic.joinus.dto.chat;

import java.time.LocalDateTime;
import java.util.List;

public record ChatMessageResponse(
        Long id,
        Long activityId,
        Long senderId,
        String senderName,
        String content,
        LocalDateTime createdAt,
        long deliveredCount,
        long seenCount,
        List<MessageReactionSummaryResponse> reactions,
        String currentUserReactionType
) {
}
