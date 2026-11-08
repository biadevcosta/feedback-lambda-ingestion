package com.feedback.ingestion.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.feedback.ingestion.application.port.out.EvaluationRepository;
import com.feedback.ingestion.domain.exception.DomainValidationException;
import com.feedback.ingestion.domain.model.Evaluation;
import com.feedback.ingestion.domain.model.Urgency;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubmitEvaluationUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-11-09T01:30:00Z");

    @Mock
    private EvaluationRepository repository;

    @Captor
    private ArgumentCaptor<Evaluation> saved;

    private SubmitEvaluationUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SubmitEvaluationUseCase(repository, () -> "eval-1", Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void should_saveEvaluationAndReturnId_when_commandIsValid() {
        // given
        var command = new SubmitEvaluationCommand("user-1", "  Great class ", 2);

        // when
        String evaluationId = useCase.execute(command);

        // then
        assertThat(evaluationId).isEqualTo("eval-1");
        verify(repository).save(saved.capture());
        Evaluation evaluation = saved.getValue();
        assertThat(evaluation.evaluationId()).isEqualTo("eval-1");
        assertThat(evaluation.userId()).isEqualTo("user-1");
        assertThat(evaluation.description().value()).isEqualTo("Great class");
        assertThat(evaluation.score().value()).isEqualTo(2);
        assertThat(evaluation.urgency()).isEqualTo(Urgency.HIGH);
        assertThat(evaluation.submittedAt()).isEqualTo(NOW);
    }

    @Test
    void should_notSave_when_scoreIsOutOfRange() {
        var command = new SubmitEvaluationCommand("user-1", "Great class", 11);

        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOf(DomainValidationException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void should_notSave_when_descriptionIsBlank() {
        var command = new SubmitEvaluationCommand("user-1", "   ", 5);

        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOf(DomainValidationException.class);
        verifyNoInteractions(repository);
    }
}
