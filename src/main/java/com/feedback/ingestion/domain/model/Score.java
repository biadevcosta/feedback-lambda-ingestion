package com.feedback.ingestion.domain.model;

import com.feedback.ingestion.domain.exception.DomainValidationException;

/** Evaluation score, an integer from 0 to 10. */
public record Score(int value) {

    public static final int MIN = 0;
    public static final int MAX = 10;

    public Score {
        if (value < MIN || value > MAX) {
            throw new DomainValidationException("score must be an integer from " + MIN + " to " + MAX);
        }
    }

    public Urgency urgency() {
        return Urgency.fromScore(value);
    }
}
