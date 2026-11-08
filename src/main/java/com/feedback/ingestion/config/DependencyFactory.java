package com.feedback.ingestion.config;

import com.feedback.ingestion.adapter.in.apigateway.SubmitEvaluationController;
import com.feedback.ingestion.adapter.in.apigateway.SubmitEvaluationRequestMapper;
import com.feedback.ingestion.adapter.out.dynamodb.DynamoDbEvaluationRepository;
import com.feedback.ingestion.application.usecase.SubmitEvaluationUseCase;
import java.time.Clock;
import java.util.UUID;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/** Composition root: builds the object graph once per execution environment. */
public final class DependencyFactory {

    private final EnvironmentConfig config;
    private final DynamoDbClient dynamoDb;

    private DependencyFactory(EnvironmentConfig config) {
        this.config = config;
        // Region comes from AWS_REGION, set by Lambda
        this.dynamoDb = DynamoDbClient.builder().httpClient(UrlConnectionHttpClient.create()).build();
    }

    public static DependencyFactory fromEnvironment() {
        return new DependencyFactory(EnvironmentConfig.fromEnvironment());
    }

    public SubmitEvaluationController submitEvaluationController() {
        var repository = new DynamoDbEvaluationRepository(dynamoDb, config.tableName());
        var useCase = new SubmitEvaluationUseCase(repository, () -> UUID.randomUUID().toString(), Clock.systemUTC());
        return new SubmitEvaluationController(useCase, new SubmitEvaluationRequestMapper());
    }
}
