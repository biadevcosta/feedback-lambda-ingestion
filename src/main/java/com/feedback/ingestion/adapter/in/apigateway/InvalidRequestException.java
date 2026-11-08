package com.feedback.ingestion.adapter.in.apigateway;

/** The HTTP request is malformed (not JSON, wrong field types); the message is client-safe. */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
