package com.amarkatha.engagement;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({NotificationProperties.class, AmarKathaMailProperties.class})
public class EngagementNotificationConfig {
}
