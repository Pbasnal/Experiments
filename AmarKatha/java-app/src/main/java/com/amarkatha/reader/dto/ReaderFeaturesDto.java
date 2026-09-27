package com.amarkatha.reader.dto;

public record ReaderFeaturesDto(
        boolean landingDiscovery,
        boolean profileProgress,
        boolean follows,
        boolean inAppNotifications,
        boolean emailNotifications
) {
}
