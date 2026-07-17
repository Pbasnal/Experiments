package com.amarkatha.admin;

public record InviteRowView(
        String token,
        String status,
        String createdByEmail,
        String expiresLabel,
        String createdLabel
) {
}
