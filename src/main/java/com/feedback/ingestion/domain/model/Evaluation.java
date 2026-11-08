package com.feedback.ingestion.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/** A student's evaluation of a class. */
public record Evaluation(String evaluationId, String userId, Description description, Score score,
        Instant submittedAt) {

    // Brasília has no daylight saving time, so a fixed UTC-3 offset defines the evaluation day
    private static final ZoneOffset BRASILIA = ZoneOffset.ofHours(-3);

    public Urgency urgency() {
        return score.urgency();
    }

    public LocalDate day() {
        return submittedAt.atOffset(BRASILIA).toLocalDate();
    }
}
