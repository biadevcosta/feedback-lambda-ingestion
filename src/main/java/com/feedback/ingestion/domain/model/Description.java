package com.feedback.ingestion.domain.model;

import com.feedback.ingestion.domain.exception.DomainValidationException;

/** Free-text evaluation, stored without surrounding whitespace. */
public record Description(String value) {

    public static final int MAX_LENGTH = 1000;

    public Description {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("description must not be empty");
        }
        value = value.strip();
        if (value.length() > MAX_LENGTH) {
            throw new DomainValidationException("description must have at most " + MAX_LENGTH + " characters");
        }
    }
}
