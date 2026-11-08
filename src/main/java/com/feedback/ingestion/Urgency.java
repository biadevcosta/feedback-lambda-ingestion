package com.feedback.ingestion;

/** Urgency level derived from the score; stored in DynamoDB as its lowercase value. */
public enum Urgency {
    HIGH("high"),
    MEDIUM("medium"),
    LOW("low");

    private final String value;

    Urgency(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    /** 0 to 3 is high, 4 to 6 is medium, 7 to 10 is low. Expects a score already validated. */
    public static Urgency fromScore(int score) {
        if (score <= 3) {
            return HIGH;
        }
        if (score <= 6) {
            return MEDIUM;
        }
        return LOW;
    }
}
