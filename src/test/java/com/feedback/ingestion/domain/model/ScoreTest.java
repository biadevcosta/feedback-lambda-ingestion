package com.feedback.ingestion.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.feedback.ingestion.domain.exception.DomainValidationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ScoreTest {

    @ParameterizedTest(name = "score {0} → {1}")
    @CsvSource({
        "0, HIGH", "1, HIGH", "2, HIGH", "3, HIGH",
        "4, MEDIUM", "5, MEDIUM", "6, MEDIUM",
        "7, LOW", "8, LOW", "9, LOW", "10, LOW"
    })
    void should_mapUrgency_when_scoreIsInRange(int value, Urgency expected) {
        assertThat(new Score(value).urgency()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "score {0}")
    @ValueSource(ints = {-1, 11, Integer.MIN_VALUE, Integer.MAX_VALUE})
    void should_reject_when_scoreIsOutOfRange(int value) {
        assertThatThrownBy(() -> new Score(value))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("score must be an integer from 0 to 10");
    }
}
