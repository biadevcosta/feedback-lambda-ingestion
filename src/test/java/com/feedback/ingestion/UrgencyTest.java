package com.feedback.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class UrgencyTest {

    @ParameterizedTest
    @CsvSource({
        "0, high", "1, high", "2, high", "3, high",
        "4, medium", "5, medium", "6, medium",
        "7, low", "8, low", "9, low", "10, low"
    })
    void mapsEveryScoreToItsUrgency(int score, String urgency) {
        assertEquals(urgency, Urgency.fromScore(score).value());
    }
}
