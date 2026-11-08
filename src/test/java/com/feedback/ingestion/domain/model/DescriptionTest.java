package com.feedback.ingestion.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.feedback.ingestion.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class DescriptionTest {

    @Test
    void should_stripWhitespace_when_created() {
        assertThat(new Description("  Great class \n").value()).isEqualTo("Great class");
    }

    @Test
    void should_accept_when_lengthIsExactlyTheLimit() {
        String text = "a".repeat(Description.MAX_LENGTH);

        assertThat(new Description(text).value()).hasSize(Description.MAX_LENGTH);
    }

    @Test
    void should_reject_when_lengthExceedsTheLimit() {
        String text = "a".repeat(Description.MAX_LENGTH + 1);

        assertThatThrownBy(() -> new Description(text))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("at most 1000");
    }

    @Test
    void should_countLengthAfterStripping_when_paddedWithSpaces() {
        String text = "  " + "a".repeat(Description.MAX_LENGTH) + "  ";

        assertThat(new Description(text).value()).hasSize(Description.MAX_LENGTH);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\n\t"})
    void should_reject_when_empty(String text) {
        assertThatThrownBy(() -> new Description(text))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("description must not be empty");
    }
}
