package com.amarkatha.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amarkatha.identity.domain.InviteToken;
import com.amarkatha.identity.domain.User;
import com.amarkatha.shared.domain.UserRole;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InviteServiceTest {

    @Mock
    private InviteTokenRepository inviteTokenRepository;

    @InjectMocks
    private InviteService inviteService;

    @Test
    void validateRejectsUsedToken() {
        InviteToken used = InviteToken.create("abc", null, null);
        used.markUsed(User.create("sub", "a@b.com", "A", UserRole.ADMIN), Instant.now());
        when(inviteTokenRepository.findByToken("abc")).thenReturn(Optional.of(used));

        InviteInvalidException ex = assertThrows(InviteInvalidException.class, () -> inviteService.validateForSignup("abc"));
        assertEquals(InviteInvalidException.InviteInvalidReason.ALREADY_USED, ex.getReason());
    }

    @Test
    void validateRejectsExpiredToken() {
        InviteToken expired = InviteToken.create("abc", null, Instant.now().minus(1, ChronoUnit.DAYS));
        when(inviteTokenRepository.findByToken("abc")).thenReturn(Optional.of(expired));

        InviteInvalidException ex = assertThrows(InviteInvalidException.class, () -> inviteService.validateForSignup("abc"));
        assertEquals(InviteInvalidException.InviteInvalidReason.EXPIRED, ex.getReason());
    }

    @Test
    void consumeMarksTokenUsed() {
        InviteToken invite = InviteToken.create("abc", null, Instant.now().plus(1, ChronoUnit.DAYS));
        User user = User.create("sub", "c@d.com", "C", UserRole.CREATOR);
        when(inviteTokenRepository.findByToken("abc")).thenReturn(Optional.of(invite));
        when(inviteTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        inviteService.consume("abc", user);

        verify(inviteTokenRepository).save(invite);
        verify(inviteTokenRepository, never()).findByToken("missing");
    }
}
