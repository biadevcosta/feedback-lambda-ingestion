package com.feedback.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IngestionHandlerTest {

    private static final String TABLE = "Evaluations";
    private static final String SUB = "user-sub-123";

    // 2026-11-09 01:30 UTC is still 2026-11-08 in Brasília (22:30)
    private static final Instant NOW = Instant.parse("2026-11-09T01:30:00Z");

    @Mock
    private DynamoDbClient dynamoDb;

    @Mock
    private Context context;

    @Mock
    private LambdaLogger logger;

    private IngestionHandler handler;

    @BeforeEach
    void setUp() {
        when(context.getLogger()).thenReturn(logger);
        handler = new IngestionHandler(dynamoDb, TABLE, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @ParameterizedTest
    @CsvSource({"0, high", "3, high", "4, medium", "6, medium", "7, low", "10, low"})
    void savesValidEvaluationWithUrgency(int score, String urgency) {
        APIGatewayV2HTTPResponse response = handle(event("{\"description\":\"Great class\",\"score\":" + score + "}", SUB));

        assertEquals(201, response.getStatusCode());
        assertTrue(response.getBody().contains("evaluationId"));

        Map<String, AttributeValue> item = savedItem();
        assertEquals(Integer.toString(score), item.get("score").n());
        assertEquals(urgency, item.get("urgency").s());
        assertEquals(SUB, item.get("userId").s());
        assertEquals("Great class", item.get("description").s());
        assertEquals("2026-11-09T01:30:00.000Z", item.get("submittedAt").s());
        assertEquals("2026-11-08", item.get("day").s());
        assertTrue(response.getBody().contains(item.get("evaluationId").s()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"description\":\"Class\",\"score\":11}",
        "{\"description\":\"Class\",\"score\":-1}",
        "{\"description\":\"Class\",\"score\":\"7\"}",
        "{\"description\":\"Class\",\"score\":true}",
        "{\"description\":\"Class\",\"score\":7.5}",
        "{\"description\":\"Class\",\"score\":7.0}",
        "{\"description\":\"Class\"}",
        "{\"description\":\"\",\"score\":5}",
        "{\"description\":\"   \",\"score\":5}",
        "{\"description\":42,\"score\":5}",
        "{\"score\":5}",
        "[1, 2]",
        "not json",
        ""
    })
    void rejectsInvalidBodyWith400(String body) {
        APIGatewayV2HTTPResponse response = handle(event(body, SUB));

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("message"));
        verify(dynamoDb, never()).putItem(any(PutItemRequest.class));
    }

    @Test
    void rejectsDescriptionOverLimit() {
        String description = "a".repeat(EvaluationRequest.MAX_DESCRIPTION_LENGTH + 1);

        APIGatewayV2HTTPResponse response = handle(event("{\"description\":\"" + description + "\",\"score\":5}", SUB));

        assertEquals(400, response.getStatusCode());
        verify(dynamoDb, never()).putItem(any(PutItemRequest.class));
    }

    @Test
    void returns401WhenSubIsMissing() {
        APIGatewayV2HTTPResponse response = handle(event("{\"description\":\"Class\",\"score\":5}", null));

        assertEquals(401, response.getStatusCode());
        verify(dynamoDb, never()).putItem(any(PutItemRequest.class));
    }

    @Test
    void returns401WhenRequestContextIsMissing() {
        APIGatewayV2HTTPEvent event = new APIGatewayV2HTTPEvent();
        event.setBody("{\"description\":\"Class\",\"score\":5}");

        assertEquals(401, handle(event).getStatusCode());
    }

    @Test
    void decodesBase64Body() {
        APIGatewayV2HTTPEvent event = event(Base64.getEncoder().encodeToString(
                "{\"description\":\"Class\",\"score\":2}".getBytes(StandardCharsets.UTF_8)), SUB);
        event.setIsBase64Encoded(true);

        assertEquals(201, handle(event).getStatusCode());
        assertEquals("high", savedItem().get("urgency").s());
    }

    @Test
    void returns500WithoutDetailsWhenDynamoDbFails() {
        when(dynamoDb.putItem(any(PutItemRequest.class)))
                .thenThrow(DynamoDbException.builder().message("internal detail").build());

        APIGatewayV2HTTPResponse response = handle(event("{\"description\":\"Secret text\",\"score\":5}", SUB));

        assertEquals(500, response.getStatusCode());
        assertFalse(response.getBody().contains("internal detail"));
        assertFalse(response.getBody().contains("Exception"));
    }

    @Test
    void neverLogsDescriptionOrUser() {
        handle(event("{\"description\":\"Secret text\",\"score\":5}", SUB));

        ArgumentCaptor<String> logged = ArgumentCaptor.forClass(String.class);
        verify(logger).log(logged.capture());
        assertFalse(logged.getValue().contains("Secret text"));
        assertFalse(logged.getValue().contains(SUB));
    }

    private APIGatewayV2HTTPResponse handle(APIGatewayV2HTTPEvent event) {
        return handler.handleRequest(event, context);
    }

    private Map<String, AttributeValue> savedItem() {
        ArgumentCaptor<PutItemRequest> request = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(dynamoDb).putItem(request.capture());
        assertEquals(TABLE, request.getValue().tableName());
        return request.getValue().item();
    }

    private static APIGatewayV2HTTPEvent event(String body, String sub) {
        APIGatewayV2HTTPEvent.RequestContext.Authorizer.JWT jwt = new APIGatewayV2HTTPEvent.RequestContext.Authorizer.JWT();
        jwt.setClaims(sub == null ? Map.of() : Map.of("sub", sub));

        APIGatewayV2HTTPEvent.RequestContext.Authorizer authorizer = new APIGatewayV2HTTPEvent.RequestContext.Authorizer();
        authorizer.setJwt(jwt);

        APIGatewayV2HTTPEvent.RequestContext requestContext = new APIGatewayV2HTTPEvent.RequestContext();
        requestContext.setAuthorizer(authorizer);

        APIGatewayV2HTTPEvent event = new APIGatewayV2HTTPEvent();
        event.setRequestContext(requestContext);
        event.setBody(body);
        return event;
    }
}
