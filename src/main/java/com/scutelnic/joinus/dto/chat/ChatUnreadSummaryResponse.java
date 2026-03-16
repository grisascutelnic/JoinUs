package com.scutelnic.joinus.dto.chat;

import java.time.LocalDateTime;
import java.util.Map;

public record ChatUnreadSummaryResponse(
        long unreadChatGroupsCount,
        Map<Long, Long> activityUnreadCounts,
        Map<Long, LocalDateTime> latestUnreadByActivity
) {
}
