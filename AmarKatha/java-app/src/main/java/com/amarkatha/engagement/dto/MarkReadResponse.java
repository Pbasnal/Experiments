package com.amarkatha.engagement.dto;

import java.time.Instant;
import java.util.UUID;

public record MarkReadResponse(UUID id, Instant readAt) {
}
