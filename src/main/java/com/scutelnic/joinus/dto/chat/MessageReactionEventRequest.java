package com.scutelnic.joinus.dto.chat;

public record MessageReactionEventRequest(
        Long messageId,
        String reactionType
) {
}
