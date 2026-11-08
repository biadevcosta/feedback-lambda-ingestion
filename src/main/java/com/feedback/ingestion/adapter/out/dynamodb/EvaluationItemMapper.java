package com.feedback.ingestion.adapter.out.dynamodb;

import com.feedback.ingestion.domain.model.Evaluation;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** Maps an Evaluation to the Evaluations table item (data contract shared by the 3 functions). */
final class EvaluationItemMapper {

    // Fixed-width timestamp so submittedAt sorts correctly as a string in the by-day index
    // (Instant.toString drops zero milliseconds, which would break the ordering)
    private static final DateTimeFormatter SUBMITTED_AT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private EvaluationItemMapper() {
    }

    static Map<String, AttributeValue> toItem(Evaluation evaluation) {
        return Map.of(
                "evaluationId", AttributeValue.fromS(evaluation.evaluationId()),
                "userId", AttributeValue.fromS(evaluation.userId()),
                "description", AttributeValue.fromS(evaluation.description().value()),
                "score", AttributeValue.fromN(Integer.toString(evaluation.score().value())),
                "urgency", AttributeValue.fromS(evaluation.urgency().value()),
                "submittedAt", AttributeValue.fromS(SUBMITTED_AT.format(evaluation.submittedAt())),
                "day", AttributeValue.fromS(evaluation.day().toString()));
    }
}
