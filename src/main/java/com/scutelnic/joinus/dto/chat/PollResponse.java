package com.scutelnic.joinus.dto.chat;

import java.time.LocalDateTime;
import java.util.List;

public record PollResponse(
        Long id,
        Long activityId,
        Long creatorId,
        String creatorName,
        String question,
        LocalDateTime createdAt,
        Long currentUserOptionId,
        List<PollOptionSummaryResponse> options
) {
}
