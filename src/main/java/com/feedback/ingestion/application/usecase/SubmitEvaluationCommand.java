package com.feedback.ingestion.application.usecase;

/** Input of SubmitEvaluationUseCase: raw values, already type-checked by the inbound adapter. */
public record SubmitEvaluationCommand(String userId, String description, int score) {
}
