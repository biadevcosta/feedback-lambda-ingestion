package com.feedback.ingestion.adapter.out.dynamodb;

import com.feedback.ingestion.application.port.out.EvaluationRepository;
import com.feedback.ingestion.domain.model.Evaluation;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

/** Saves evaluations with PutItem, the only permission this function has. */
public final class DynamoDbEvaluationRepository implements EvaluationRepository {

    private final DynamoDbClient client;
    private final String tableName;

    public DynamoDbEvaluationRepository(DynamoDbClient client, String tableName) {
        this.client = client;
        this.tableName = tableName;
    }

    @Override
    public void save(Evaluation evaluation) {
        client.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(EvaluationItemMapper.toItem(evaluation))
                .build());
    }
}
