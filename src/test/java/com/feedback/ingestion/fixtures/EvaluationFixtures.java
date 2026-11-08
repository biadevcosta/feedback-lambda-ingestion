package com.feedback.ingestion.fixtures;

import com.feedback.ingestion.domain.model.Description;
import com.feedback.ingestion.domain.model.Evaluation;
import com.feedback.ingestion.domain.model.Score;
import java.time.Instant;

/** Test Data Builder: valid defaults, tests override only what matters to them. */
public final class EvaluationFixtures {

    public static final String EVALUATION_ID = "eval-1";
    public static final String USER_ID = "user-1";
    // 01:30 UTC on Nov 9 is still Nov 8 in Brasília (22:30)
    public static final Instant SUBMITTED_AT = Instant.parse("2026-11-09T01:30:00Z");

    private String evaluationId = EVALUATION_ID;
    private String userId = USER_ID;
    private String description = "Great class";
    private int score = 7;
    private Instant submittedAt = SUBMITTED_AT;

    private EvaluationFixtures() {
    }

    public static EvaluationFixtures anEvaluation() {
        return new EvaluationFixtures();
    }

    public EvaluationFixtures withScore(int score) {
        this.score = score;
        return this;
    }

    public EvaluationFixtures withDescription(String description) {
        this.description = description;
        return this;
    }

    public EvaluationFixtures withSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
        return this;
    }

    public Evaluation build() {
        return new Evaluation(evaluationId, userId, new Description(description), new Score(score), submittedAt);
    }
}
