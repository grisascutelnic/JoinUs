package com.scutelnic.joinus.dto.chat;

import java.util.List;

public record PollCreateRequest(
        String question,
        List<String> options
) {
}
