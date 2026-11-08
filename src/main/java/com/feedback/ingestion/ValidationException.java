package com.feedback.ingestion;

/** Invalid request body; its message is safe to return to the client as a 400. */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
