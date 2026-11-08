package com.feedback.ingestion.adapter.in.apigateway;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

/** JSON responses for the HTTP API. */
final class HttpResponses {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private HttpResponses() {
    }

    static APIGatewayV2HTTPResponse created(Map<String, String> body) {
        return json(201, body);
    }

    static APIGatewayV2HTTPResponse badRequest(String message) {
        return json(400, Map.of("message", message));
    }

    static APIGatewayV2HTTPResponse unauthorized() {
        return json(401, Map.of("message", "Unauthorized"));
    }

    static APIGatewayV2HTTPResponse internalError() {
        return json(500, Map.of("message", "Could not save the evaluation"));
    }

    private static APIGatewayV2HTTPResponse json(int statusCode, Map<String, String> body) {
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
