package com.scutelnic.joinus.dto.chat;

import java.time.LocalDateTime;

public record SeenUserResponse(
        Long userId,
        String fullName,
        LocalDateTime seenAt
) {
}
