package com.scutelnic.joinus.dto.chat;

public record PollEditRequest(
        Long pollId,
        String question
) {
}
