package com.scutelnic.joinus.dto.chat;

public record MessageReactionSummaryResponse(
        String reactionType,
        String emoji,
        long count,
        boolean reactedByCurrentUser
) {
}
