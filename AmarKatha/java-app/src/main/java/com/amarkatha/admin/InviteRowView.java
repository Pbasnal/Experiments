package com.amarkatha.admin;

public record InviteRowView(
        String token,
        String status,
        String usesLabel,
        String createdByEmail,
        String redeemedByLabel,
        String expiresLabel,
        String createdLabel
) {
}
