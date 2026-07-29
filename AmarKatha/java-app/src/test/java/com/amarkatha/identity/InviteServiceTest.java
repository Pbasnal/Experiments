package com.amarkatha.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amarkatha.identity.domain.InviteRedemption;
import com.amarkatha.identity.domain.InviteToken;
import com.amarkatha.identity.domain.User;
import com.amarkatha.shared.domain.UserRole;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InviteServiceTest {

    @Mock
    private InviteTokenRepository inviteTokenRepository;

    @Mock
    private InviteRedemptionRepository inviteRedemptionRepository;

    @InjectMocks
    private InviteService inviteService;

    @Test
    void validateRejectsExhaustedToken() {
        InviteToken used = InviteToken.create("abc", null, null, 1);
        used.applyConsumed(User.create("sub", "a@b.com", "A", UserRole.ADMIN), Instant.now());
        when(inviteTokenRepository.findByToken("abc")).thenReturn(Optional.of(used));

        InviteInvalidException ex = assertThrows(InviteInvalidException.class, () -> inviteService.validateForSignup("abc"));
        assertEquals(InviteInvalidException.InviteInvalidReason.EXHAUSTED, ex.getReason());
    }

    @Test
    void validateRejectsExpiredToken() {
        InviteToken expired = InviteToken.create("abc", null, Instant.now().minus(1, ChronoUnit.DAYS), 5);
        when(inviteTokenRepository.findByToken("abc")).thenReturn(Optional.of(expired));

        InviteInvalidException ex = assertThrows(InviteInvalidException.class, () -> inviteService.validateForSignup("abc"));
        assertEquals(InviteInvalidException.InviteInvalidReason.EXPIRED, ex.getReason());
    }

    @Test
    void consumeIncrementsUseAtomically() {
        InviteToken invite = InviteToken.create("abc", null, Instant.now().plus(1, ChronoUnit.DAYS), 1);
        User user = User.create("sub", "c@d.com", "C", UserRole.CREATOR);
        when(inviteTokenRepository.findByToken("abc")).thenReturn(Optional.of(invite));
        when(inviteTokenRepository.tryConsume(eq(invite.getId()), eq(user.getId()), any(Instant.class)))
                .thenReturn(1);

        inviteService.consume("abc", user);

        verify(inviteTokenRepository).tryConsume(eq(invite.getId()), eq(user.getId()), any(Instant.class));
        assertEquals(1, invite.getUseCount());
        verify(inviteTokenRepository, never()).save(any());

        ArgumentCaptor<InviteRedemption> redemptionCaptor = ArgumentCaptor.forClass(InviteRedemption.class);
        verify(inviteRedemptionRepository).save(redemptionCaptor.capture());
        assertEquals(invite.getId(), redemptionCaptor.getValue().getInviteToken().getId());
        assertEquals(user.getId(), redemptionCaptor.getValue().getUser().getId());
    }

    @Test
    void consumeFailsWhenAtomicUpdateMisses() {
        InviteToken invite = InviteToken.create("abc", null, Instant.now().plus(1, ChronoUnit.DAYS), 1);
        User user = User.create("sub", "c@d.com", "C", UserRole.CREATOR);
        when(inviteTokenRepository.findByToken("abc")).thenReturn(Optional.of(invite));
        when(inviteTokenRepository.tryConsume(eq(invite.getId()), eq(user.getId()), any(Instant.class)))
                .thenReturn(0);

        InviteInvalidException ex = assertThrows(
                InviteInvalidException.class,
                () -> inviteService.consume("abc", user)
        );
        assertEquals(InviteInvalidException.InviteInvalidReason.EXHAUSTED, ex.getReason());
        verify(inviteRedemptionRepository, never()).save(any());
    }

    @Test
    void multiUseAllowsUntilCap() {
        InviteToken invite = InviteToken.create("abc", null, Instant.now().plus(1, ChronoUnit.DAYS), 3);
        User user = User.create("sub", "c@d.com", "C", UserRole.CREATOR);
        when(inviteTokenRepository.findByToken("abc")).thenReturn(Optional.of(invite));
        when(inviteTokenRepository.tryConsume(eq(invite.getId()), eq(user.getId()), any(Instant.class)))
                .thenReturn(1, 1, 1, 0);

        inviteService.consume("abc", user);
        inviteService.consume("abc", user);
        inviteService.consume("abc", user);

        assertEquals(3, invite.getUseCount());
        assertThrows(InviteInvalidException.class, () -> inviteService.consume("abc", user));
        verify(inviteRedemptionRepository, org.mockito.Mockito.times(3)).save(any(InviteRedemption.class));
    }

    @Test
    void generateRejectsOutOfRangeMaxUses() {
        User admin = User.create("sub", "a@b.com", "A", UserRole.ADMIN);
        assertThrows(IllegalArgumentException.class, () -> inviteService.generate(admin, 0));
        assertThrows(IllegalArgumentException.class, () -> inviteService.generate(admin, 101));
    }
}
