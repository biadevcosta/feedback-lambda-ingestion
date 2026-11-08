package com.feedback.ingestion.domain.exception;

/** A business invariant was violated; the message is written to be safe to show to the client. */
public class DomainValidationException extends RuntimeException {

    public DomainValidationException(String message) {
        super(message);
    }
}
