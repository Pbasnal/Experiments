package com.amarkatha.admin;

import com.amarkatha.identity.InviteService;
import com.amarkatha.identity.UserRepository;
import com.amarkatha.identity.domain.InviteRedemption;
import com.amarkatha.identity.domain.InviteToken;
import com.amarkatha.shared.domain.UserRole;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(IST);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm").withZone(IST);

    private final InviteService inviteService;
    private final UserRepository userRepository;

    public AdminDashboardService(InviteService inviteService, UserRepository userRepository) {
        this.inviteService = inviteService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardView dashboard() {
        Instant now = Instant.now();
        List<InviteRowView> recent = listInviteRows(null).stream().limit(5).toList();
        return new AdminDashboardView(
                inviteService.countAvailable(),
                inviteService.countUsedSince(now.minus(30, ChronoUnit.DAYS)),
                userRepository.countByRole(UserRole.CREATOR),
                recent
        );
    }

    @Transactional(readOnly = true)
    public List<InviteRowView> listInviteRows(String statusFilter) {
        Instant now = Instant.now();
        String filter = statusFilter == null ? "all" : statusFilter.trim().toLowerCase(Locale.ROOT);
        List<InviteToken> invites = inviteService.listRecentWithCreator();
        Map<UUID, List<String>> emailsByInvite = redemptionEmailsByInvite(invites);
        return invites.stream()
                .map(invite -> toRow(invite, now, emailsByInvite.getOrDefault(invite.getId(), List.of())))
                .filter(row -> matchesFilter(row.status(), filter))
                .toList();
    }

    private Map<UUID, List<String>> redemptionEmailsByInvite(List<InviteToken> invites) {
        List<UUID> ids = invites.stream().map(InviteToken::getId).toList();
        Map<UUID, List<String>> emailsByInvite = new LinkedHashMap<>();
        for (InviteRedemption redemption : inviteService.listRedemptionsForInvites(ids)) {
            emailsByInvite
                    .computeIfAbsent(redemption.getInviteToken().getId(), ignored -> new ArrayList<>())
                    .add(redemption.getUser().getEmail());
        }
        return emailsByInvite;
    }

    private static InviteRowView toRow(InviteToken invite, Instant now, List<String> redeemedEmails) {
        String createdByEmail = invite.getCreatedBy() != null ? invite.getCreatedBy().getEmail() : "-";
        String redeemedByLabel = redeemedEmails.isEmpty()
                ? "-"
                : redeemedEmails.stream().collect(Collectors.joining(", "));
        return new InviteRowView(
                invite.getToken(),
                statusOf(invite, now),
                invite.getUseCount() + " / " + invite.getMaxUses(),
                createdByEmail,
                redeemedByLabel,
                invite.getExpiresAt() != null ? DATE.format(invite.getExpiresAt()) : "-",
                DATE_TIME.format(invite.getCreatedAt())
        );
    }

    private static String statusOf(InviteToken invite, Instant now) {
        if (invite.isExpired(now)) {
            return "Expired";
        }
        if (invite.isExhausted()) {
            return "Exhausted";
        }
        return "Available";
    }

    private static boolean matchesFilter(String status, String filter) {
        return switch (filter) {
            case "available" -> "Available".equals(status);
            case "used", "exhausted" -> "Exhausted".equals(status);
            case "expired" -> "Expired".equals(status);
            default -> true;
        };
    }
}
