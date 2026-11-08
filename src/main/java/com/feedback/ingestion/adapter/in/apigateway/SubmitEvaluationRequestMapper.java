package com.feedback.ingestion.adapter.in.apigateway;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.feedback.ingestion.application.usecase.SubmitEvaluationCommand;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * Translates the HTTP API event into a command. Checks only the transport format (JSON, field
 * types); business rules such as the score range live in the domain.
 */
public final class SubmitEvaluationRequestMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public SubmitEvaluationCommand toCommand(APIGatewayV2HTTPEvent event) {
        String userId = userId(event).orElseThrow(UnauthenticatedException::new);
        JsonNode root = parseObject(body(event));

        JsonNode description = root.get("description");
        if (description == null || !description.isTextual()) {
            throw new InvalidRequestException("description is required and must be a string");
        }

        // isIntegralNumber rejects text ("7"), booleans and decimals (7.5, 7.0)
        JsonNode score = root.get("score");
        if (score == null || !score.isIntegralNumber() || !score.canConvertToInt()) {
            throw new InvalidRequestException("score is required and must be an integer");
        }

        return new SubmitEvaluationCommand(userId, description.asText(), score.asInt());
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

    private static JsonNode parseObject(String body) {
        if (body == null || body.isBlank()) {
            throw new InvalidRequestException("Request body is required");
        }
        try {
            JsonNode root = MAPPER.readTree(body);
            if (root == null || !root.isObject()) {
                throw new InvalidRequestException("Request body must be a JSON object");
            }
            return root;
        } catch (JsonProcessingException e) {
            throw new InvalidRequestException("Request body must be valid JSON");
        }
    }
}
