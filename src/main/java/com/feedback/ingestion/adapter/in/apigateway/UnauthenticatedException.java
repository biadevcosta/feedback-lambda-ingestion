package com.feedback.ingestion.adapter.in.apigateway;

/** The event has no authenticated user (missing JWT "sub" claim). */
public class UnauthenticatedException extends RuntimeException {

    public UnauthenticatedException() {
        super("Missing authenticated user");
    }
}
