package com.scutelnic.joinus.dto.chat;

import java.util.List;

public record MessageSeenSummaryResponse(
        Long messageId,
        List<SeenUserResponse> seenBy
) {
}
