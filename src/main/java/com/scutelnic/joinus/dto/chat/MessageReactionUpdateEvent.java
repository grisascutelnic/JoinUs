package com.scutelnic.joinus.dto.chat;

import java.util.List;

public record MessageReactionUpdateEvent(
        Long messageId,
        List<MessageReactionSummaryResponse> reactions,
        Long actorUserId,
        String actorReactionType
) {
}
