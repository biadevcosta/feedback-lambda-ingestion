package com.feedback.ingestion.application.port.out;

/** Generates unique evaluation ids; injected so tests get predictable ids. */
@FunctionalInterface
public interface IdGenerator {

    String newId();
}
