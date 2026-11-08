package com.feedback.ingestion.config;

/** Environment variables set by Terraform; the only place that reads them. */
public record EnvironmentConfig(String tableName) {

    public static EnvironmentConfig fromEnvironment() {
        return new EnvironmentConfig(required("TABLE_NAME"));
    }

    // Fails at init instead of on the first request when the function is misconfigured
    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing environment variable " + name);
        }
        return value;
    }
}
