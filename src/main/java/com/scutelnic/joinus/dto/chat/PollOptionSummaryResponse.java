package com.scutelnic.joinus.dto.chat;

import java.util.List;

public record PollOptionSummaryResponse(
        Long optionId,
        String text,
        long voteCount,
        boolean votedByCurrentUser,
        List<PollVoterResponse> voters
) {
}
