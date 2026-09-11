package com.example.guestbook.message;

import java.time.OffsetDateTime;

public record MessageResponse (Long id, String name, String message, OffsetDateTime createdAt) {

    static MessageResponse from(Message m) {
        return new MessageResponse(m.getId(), m.getName(), m.getBody(), m.getCreatedAt());
    }

}
