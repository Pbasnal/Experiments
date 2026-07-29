package com.amarkatha.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amarkatha.identity.domain.User;
import com.amarkatha.shared.domain.UserRole;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

@ExtendWith(MockitoExtension.class)
class UserOnboardingServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private InviteService inviteService;
    @Mock
    private FeatureFlagService featureFlagService;

    private UserOnboardingService service;

    @BeforeEach
    void setUp() {
        AmarKathaProperties properties = new AmarKathaProperties(
                "hello@amarkatha.in",
                new AmarKathaProperties.Admin("admin@amarkatha.in")
        );
        service = new UserOnboardingService(userRepository, inviteService, featureFlagService, properties);
    }

    @Test
    void googleAuthCreatesBootstrapAdminWithoutInvite() {
        when(userRepository.findByGoogleSub("sub-1")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User user = service.completeOAuthLogin(oauthUser("sub-1", "admin@amarkatha.in"), OAuthIntent.GOOGLE_AUTH, null);

        assertEquals(UserRole.ADMIN, user.getRole());
        verify(inviteService, never()).consume(any(), any());
    }

    @Test
    void googleAuthRequiresInviteForNewCreator() {
        when(userRepository.findByGoogleSub("sub-2")).thenReturn(Optional.empty());
        when(featureFlagService.isInviteRequired()).thenReturn(true);

        OAuthOnboardingException ex = assertThrows(
                OAuthOnboardingException.class,
                () -> service.completeOAuthLogin(oauthUser("sub-2", "creator@example.com"), OAuthIntent.GOOGLE_AUTH, null)
        );
        assertEquals(OAuthOnboardingException.Reason.INVITE_REQUIRED, ex.getReason());
    }

    @Test
    void existingCreatorPromotedWhenBootstrapEmail() {
        User existing = User.create("sub-3", "admin@amarkatha.in", "Admin", UserRole.CREATOR);
        when(userRepository.findByGoogleSub("sub-3")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User user = service.completeOAuthLogin(oauthUser("sub-3", "admin@amarkatha.in"), OAuthIntent.CREATOR_LOGIN, null);

        assertEquals(UserRole.ADMIN, user.getRole());
    }

    @Test
    void creatorSignupConsumesInvite() {
        when(userRepository.findByGoogleSub("sub-4")).thenReturn(Optional.empty());
        when(featureFlagService.isInviteRequired()).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User user = service.completeOAuthLogin(
                oauthUser("sub-4", "creator@example.com"),
                OAuthIntent.CREATOR_SIGNUP,
                "invite-token"
        );

        assertEquals(UserRole.CREATOR, user.getRole());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(inviteService).consume(org.mockito.ArgumentMatchers.eq("invite-token"), userCaptor.capture());
        assertEquals("creator@example.com", userCaptor.getValue().getEmail());
    }

    @Test
    void creatorSignupMapsExpiredInvite() {
        when(userRepository.findByGoogleSub("sub-5")).thenReturn(Optional.empty());
        when(featureFlagService.isInviteRequired()).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inviteService.consume(any(), any())).thenThrow(
                new InviteInvalidException(InviteInvalidException.InviteInvalidReason.EXPIRED)
        );

        OAuthOnboardingException ex = assertThrows(
                OAuthOnboardingException.class,
                () -> service.completeOAuthLogin(
                        oauthUser("sub-5", "creator@example.com"),
                        OAuthIntent.CREATOR_SIGNUP,
                        "stale-token"
                )
        );
        assertEquals(OAuthOnboardingException.Reason.INVITE_EXPIRED, ex.getReason());
    }

    @Test
    void creatorSignupMapsExhaustedInvite() {
        when(userRepository.findByGoogleSub("sub-6")).thenReturn(Optional.empty());
        when(featureFlagService.isInviteRequired()).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inviteService.consume(any(), any())).thenThrow(
                new InviteInvalidException(InviteInvalidException.InviteInvalidReason.EXHAUSTED)
        );

        OAuthOnboardingException ex = assertThrows(
                OAuthOnboardingException.class,
                () -> service.completeOAuthLogin(
                        oauthUser("sub-6", "creator@example.com"),
                        OAuthIntent.CREATOR_SIGNUP,
                        "used-token"
                )
        );
        assertEquals(OAuthOnboardingException.Reason.INVITE_EXHAUSTED, ex.getReason());
    }

    @Test
    void creatorSignupMapsInvalidInvite() {
        when(userRepository.findByGoogleSub("sub-7")).thenReturn(Optional.empty());
        when(featureFlagService.isInviteRequired()).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inviteService.consume(any(), any())).thenThrow(
                new InviteInvalidException(InviteInvalidException.InviteInvalidReason.NOT_FOUND)
        );

        OAuthOnboardingException ex = assertThrows(
                OAuthOnboardingException.class,
                () -> service.completeOAuthLogin(
                        oauthUser("sub-7", "creator@example.com"),
                        OAuthIntent.CREATOR_SIGNUP,
                        "missing-token"
                )
        );
        assertEquals(OAuthOnboardingException.Reason.INVITE_INVALID, ex.getReason());
    }

    private static OAuth2User oauthUser(String sub, String email) {
        return new DefaultOAuth2User(
                java.util.List.of(),
                Map.of("sub", sub, "email", email, "name", "Test User"),
                "sub"
        );
    }
}
