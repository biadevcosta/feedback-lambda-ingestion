package com.feedback.ingestion.application.port.out;

import com.feedback.ingestion.domain.model.Evaluation;

/** Persists evaluations. */
public interface EvaluationRepository {

    void save(Evaluation evaluation);
}
