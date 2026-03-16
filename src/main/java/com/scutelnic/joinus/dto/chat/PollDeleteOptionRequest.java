package com.scutelnic.joinus.dto.chat;

public record PollDeleteOptionRequest(
        Long pollId,
        Long optionId
) {
}
