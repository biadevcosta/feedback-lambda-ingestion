package com.feedback.ingestion.domain.model;

import static com.feedback.ingestion.fixtures.EvaluationFixtures.anEvaluation;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EvaluationTest {

    @ParameterizedTest(name = "{0} UTC → day {1}")
    @CsvSource({
        "2026-11-09T01:30:00Z, 2026-11-08",
        "2026-11-09T02:59:59.999Z, 2026-11-08",
        "2026-11-09T03:00:00Z, 2026-11-09",
        "2026-11-09T23:59:59Z, 2026-11-09"
    })
    void should_useBrasiliaDate_when_computingDay(String submittedAt, String expectedDay) {
        Evaluation evaluation = anEvaluation().withSubmittedAt(Instant.parse(submittedAt)).build();

        assertThat(evaluation.day()).isEqualTo(LocalDate.parse(expectedDay));
    }

    @Test
    void should_deriveUrgencyFromScore() {
        assertThat(anEvaluation().withScore(2).build().urgency()).isEqualTo(Urgency.HIGH);
    }
}
