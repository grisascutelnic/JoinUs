package com.scutelnic.joinus.dto.chat;

public record PollEditOptionRequest(
        Long pollId,
        Long optionId,
        String optionText
) {
}
