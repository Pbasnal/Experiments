package com.amarkatha.admin;

import java.time.Instant;
import java.util.List;

public record AdminDashboardView(
        long availableInvites,
        long usedInvitesLast30Days,
        long creatorCount,
        List<InviteRowView> recentInvites
) {
}
