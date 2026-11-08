package com.feedback.ingestion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Validated POST /evaluations body. */
public record EvaluationRequest(String description, int score) {

    public static final int MAX_DESCRIPTION_LENGTH = 1000;
    public static final int MIN_SCORE = 0;
    public static final int MAX_SCORE = 10;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Parses and validates the JSON body, throwing ValidationException with a client-safe message. */
    public static EvaluationRequest parse(String body) {
        if (body == null || body.isBlank()) {
            throw new ValidationException("Request body is required");
        }

        JsonNode root;
        try {
            root = MAPPER.readTree(body);
        } catch (JsonProcessingException e) {
            throw new ValidationException("Request body must be valid JSON");
        }
        if (root == null || !root.isObject()) {
            throw new ValidationException("Request body must be a JSON object");
        }

        JsonNode descriptionNode = root.get("description");
        if (descriptionNode == null || !descriptionNode.isTextual() || descriptionNode.asText().isBlank()) {
            throw new ValidationException("description is required and must be a non-empty string");
        }
        String description = descriptionNode.asText().strip();
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new ValidationException("description must have at most " + MAX_DESCRIPTION_LENGTH + " characters");
        }

        // isIntegralNumber rejects text ("7"), booleans and decimals (7.5, 7.0)
        JsonNode scoreNode = root.get("score");
        if (scoreNode == null || !scoreNode.isIntegralNumber() || !scoreNode.canConvertToInt()
                || scoreNode.asInt() < MIN_SCORE || scoreNode.asInt() > MAX_SCORE) {
            throw new ValidationException(
                    "score is required and must be an integer from " + MIN_SCORE + " to " + MAX_SCORE);
        }

        return new EvaluationRequest(description, scoreNode.asInt());
    }
}
