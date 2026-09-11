package com.example.guestbook.message;

public class InvalidPasscodeException extends RuntimeException {

    public InvalidPasscodeException() {
        super("Invalid passcode");
    }
}
