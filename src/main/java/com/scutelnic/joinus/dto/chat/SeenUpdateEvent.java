package com.scutelnic.joinus.dto.chat;

public record SeenUpdateEvent(Long messageId, long deliveredCount, long seenCount) {
}
