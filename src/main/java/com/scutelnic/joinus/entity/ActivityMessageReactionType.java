package com.scutelnic.joinus.entity;

import java.util.Locale;

public enum ActivityMessageReactionType {
    LIKE("👍"),
    LOVE("❤️"),
    LAUGH("😂"),
    WOW("😮");

    private final String emoji;

    ActivityMessageReactionType(String emoji) {
        this.emoji = emoji;
    }

    public String getEmoji() {
        return emoji;
    }

    public static ActivityMessageReactionType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ActivityMessageReactionType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
