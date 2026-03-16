package com.scutelnic.joinus.dto.chat;

import java.time.LocalDateTime;

public record PollVoterResponse(
        Long userId,
        String userName,
        LocalDateTime votedAt
) {
}
