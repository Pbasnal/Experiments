package com.amarkatha.engagement.dto;

public record NotificationCapabilitiesDto(
        boolean inAppAvailable,
        boolean emailAvailable,
        boolean pushAvailable
) {
}
