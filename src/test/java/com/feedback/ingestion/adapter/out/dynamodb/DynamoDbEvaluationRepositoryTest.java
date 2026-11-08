package com.feedback.ingestion.adapter.out.dynamodb;

import static com.feedback.ingestion.fixtures.EvaluationFixtures.anEvaluation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

@ExtendWith(MockitoExtension.class)
class DynamoDbEvaluationRepositoryTest {

    @Mock
    private DynamoDbClient client;

    @Captor
    private ArgumentCaptor<PutItemRequest> request;

    @Test
    void should_putItemFollowingTheDataContract_when_saving() {
        // given
        var repository = new DynamoDbEvaluationRepository(client, "Evaluations");

        // when
        repository.save(anEvaluation().withScore(2).build());

        // then
        verify(client).putItem(request.capture());
        assertThat(request.getValue().tableName()).isEqualTo("Evaluations");
        assertThat(request.getValue().item()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "evaluationId", AttributeValue.fromS("eval-1"),
                "userId", AttributeValue.fromS("user-1"),
                "description", AttributeValue.fromS("Great class"),
                "score", AttributeValue.fromN("2"),
                "urgency", AttributeValue.fromS("high"),
                "submittedAt", AttributeValue.fromS("2026-11-09T01:30:00.000Z"),
                "day", AttributeValue.fromS("2026-11-08")));
    }

    @Test
    void should_keepMillisecondsInSubmittedAt_when_theyAreZero() {
        // fixed width keeps string ordering correct in the by-day index
        var evaluation = anEvaluation().withSubmittedAt(Instant.parse("2026-11-09T12:00:00Z")).build();

        assertThat(EvaluationItemMapper.toItem(evaluation).get("submittedAt").s())
                .isEqualTo("2026-11-09T12:00:00.000Z");
    }
}
