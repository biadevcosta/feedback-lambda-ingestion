package com.feedback.ingestion.application.usecase;

import com.feedback.ingestion.application.port.out.EvaluationRepository;
import com.feedback.ingestion.application.port.out.IdGenerator;
import com.feedback.ingestion.domain.model.Description;
import com.feedback.ingestion.domain.model.Evaluation;
import com.feedback.ingestion.domain.model.Score;
import java.time.Clock;

/** Validates a student's evaluation and saves it. */
public final class SubmitEvaluationUseCase {

    private final EvaluationRepository repository;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public SubmitEvaluationUseCase(EvaluationRepository repository, IdGenerator idGenerator, Clock clock) {
        this.repository = repository;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    /**
     * Returns the id of the saved evaluation.
     *
     * @throws com.feedback.ingestion.domain.exception.DomainValidationException if description or score is invalid
     */
    public String execute(SubmitEvaluationCommand command) {
        Evaluation evaluation = new Evaluation(
                idGenerator.newId(),
                command.userId(),
                new Description(command.description()),
                new Score(command.score()),
                clock.instant());
        repository.save(evaluation);
        return evaluation.evaluationId();
    }
}
