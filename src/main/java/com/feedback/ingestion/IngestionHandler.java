package com.feedback.ingestion;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

/**
 * Receives POST /evaluations (HTTP API, payload v2, JWT already validated by the
 * Cognito authorizer) and writes the evaluation to the Evaluations table.
 */
public class IngestionHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    // Brasília has no daylight saving time, so a fixed UTC-3 offset defines the "day" attribute
    private static final ZoneOffset BRASILIA = ZoneOffset.ofHours(-3);

    // Fixed-width timestamp so submittedAt sorts correctly as a string in the by-day index
    // (Instant.toString drops zero milliseconds, which would break the ordering)
    private static final DateTimeFormatter SUBMITTED_AT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final DynamoDbClient dynamoDb;
    private final String tableName;
    private final Clock clock;

    /** Used by the Lambda runtime; the client is created once per execution environment. */
    public IngestionHandler() {
        this(DynamoDbClient.builder().httpClient(UrlConnectionHttpClient.create()).build(),
                System.getenv("TABLE_NAME"),
                Clock.systemUTC());
    }

    IngestionHandler(DynamoDbClient dynamoDb, String tableName, Clock clock) {
        this.dynamoDb = dynamoDb;
        this.tableName = tableName;
        this.clock = clock;
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        Optional<String> userId = userId(event);
        if (userId.isEmpty()) {
            return response(401, Map.of("message", "Unauthorized"));
        }

        EvaluationRequest request;
        try {
            request = EvaluationRequest.parse(body(event));
        } catch (ValidationException e) {
            return response(400, Map.of("message", e.getMessage()));
        }

        Instant now = clock.instant();
        String evaluationId = UUID.randomUUID().toString();
        Map<String, AttributeValue> item = Map.of(
                "evaluationId", AttributeValue.fromS(evaluationId),
                "userId", AttributeValue.fromS(userId.get()),
                "description", AttributeValue.fromS(request.description()),
                "score", AttributeValue.fromN(Integer.toString(request.score())),
                "urgency", AttributeValue.fromS(Urgency.fromScore(request.score()).value()),
                "submittedAt", AttributeValue.fromS(SUBMITTED_AT.format(now)),
                "day", AttributeValue.fromS(now.atOffset(BRASILIA).toLocalDate().toString()));

        try {
            dynamoDb.putItem(PutItemRequest.builder().tableName(tableName).item(item).build());
        } catch (RuntimeException e) {
            // LGPD: log only the id and the error type, never the description or the user
            context.getLogger().log("Failed to save evaluation " + evaluationId + ": " + e.getClass().getSimpleName());
            return response(500, Map.of("message", "Could not save the evaluation"));
        }

        context.getLogger().log("Saved evaluation " + evaluationId);
        return response(201, Map.of("evaluationId", evaluationId));
    }

    private static Optional<String> userId(APIGatewayV2HTTPEvent event) {
        return Optional.ofNullable(event.getRequestContext())
                .map(APIGatewayV2HTTPEvent.RequestContext::getAuthorizer)
                .map(APIGatewayV2HTTPEvent.RequestContext.Authorizer::getJwt)
                .map(APIGatewayV2HTTPEvent.RequestContext.Authorizer.JWT::getClaims)
                .map(claims -> claims.get("sub"))
                .filter(sub -> !sub.isBlank());
    }

    // HTTP API base64-encodes bodies it doesn't treat as text
    private static String body(APIGatewayV2HTTPEvent event) {
        String body = event.getBody();
        if (body != null && event.getIsBase64Encoded()) {
            return new String(Base64.getDecoder().decode(body), StandardCharsets.UTF_8);
        }
        return body;
    }

    private static APIGatewayV2HTTPResponse response(int statusCode, Map<String, String> body) {
        try {
            return APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(statusCode)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(MAPPER.writeValueAsString(body))
                    .build();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize response", e);
        }
    }
}
