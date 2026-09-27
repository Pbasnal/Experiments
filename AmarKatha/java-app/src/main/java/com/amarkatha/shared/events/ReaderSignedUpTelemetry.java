package com.amarkatha.shared.events;

/**
 * Fired when a new READER account is created via OAuth.
 * Carries only the anonymous {@code reader_id} cookie value — no account PII.
 */
public record ReaderSignedUpTelemetry(String anonymousReaderId) {
}
