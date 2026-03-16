package com.scutelnic.joinus.dto.chat;

public record PollAddOptionRequest(
        Long pollId,
        String optionText
) {
}
