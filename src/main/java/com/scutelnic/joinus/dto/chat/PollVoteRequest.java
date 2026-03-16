package com.scutelnic.joinus.dto.chat;

public record PollVoteRequest(
        Long pollId,
        Long optionId
) {
}
