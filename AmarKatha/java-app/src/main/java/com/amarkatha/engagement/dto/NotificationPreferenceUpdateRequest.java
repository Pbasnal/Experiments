package com.amarkatha.engagement.dto;

public record NotificationPreferenceUpdateRequest(
        Boolean emailNewChapter,
        Boolean inAppNewChapter,
        Boolean emailProductUpdates
) {
}
