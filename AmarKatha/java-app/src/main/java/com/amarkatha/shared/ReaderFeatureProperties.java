package com.amarkatha.shared;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Staged reader-portal rollout gates. Env-overridable; anonymous reading stays up
 * when discovery/profile/follows/notifications are disabled.
 */
@ConfigurationProperties(prefix = "amarkatha.reader.features")
public record ReaderFeatureProperties(
        @DefaultValue("true") boolean landingDiscovery,
        @DefaultValue("true") boolean profileProgress,
        @DefaultValue("true") boolean follows,
        @DefaultValue("true") boolean inAppNotifications,
        @DefaultValue("false") boolean emailNotifications
) {
}
