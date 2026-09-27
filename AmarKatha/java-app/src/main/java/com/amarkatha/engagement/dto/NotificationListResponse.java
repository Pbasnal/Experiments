package com.amarkatha.engagement.dto;

import java.util.List;

public record NotificationListResponse(
        List<ReaderNotificationDto> items,
        long unreadCount
) {
}
