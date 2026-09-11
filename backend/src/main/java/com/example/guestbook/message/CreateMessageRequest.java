package com.example.guestbook.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMessageRequest (
    @NotBlank @Size(max = 200) String name,
    @NotBlank @Size(max = 1000) String message,
    @NotBlank String passcode) {
}
