package com.scutelnic.joinus.dto.chat;

public record PollUpdateEvent(
        String action,
        Long pollId,
        PollResponse poll
) {
}
