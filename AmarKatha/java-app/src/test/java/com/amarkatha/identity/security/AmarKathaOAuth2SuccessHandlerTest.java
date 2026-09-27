package com.amarkatha.identity.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amarkatha.identity.OAuthIntent;
import com.amarkatha.identity.OAuthLoginResult;
import com.amarkatha.identity.UserOnboardingService;
import com.amarkatha.identity.domain.User;
import com.amarkatha.shared.domain.UserRole;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.SecurityContextRepository;

@ExtendWith(MockitoExtension.class)
class AmarKathaOAuth2SuccessHandlerTest {

    @Mock
    private UserOnboardingService userOnboardingService;
    @Mock
    private SecurityContextRepository securityContextRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AmarKathaOAuth2SuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new AmarKathaOAuth2SuccessHandler(
                userOnboardingService,
                securityContextRepository,
                eventPublisher
        );
    }

    @Test
    void pendingFollowRedirectsToSafeSpaPathAndKeepsSessionSlug() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthSessionKeys.OAUTH_INTENT, OAuthIntent.READER_LOGIN.name());
        session.setAttribute(AuthSessionKeys.PENDING_FOLLOW_SERIES_SLUG, "demo-series");
        session.setAttribute(AuthSessionKeys.OAUTH_RETURN_TO, "/read/s/demo-series");

        User user = User.create("g1", "r@example.com", "Reader", UserRole.READER);
        when(userOnboardingService.completeOAuthLogin(any(), eq(OAuthIntent.READER_LOGIN), eq(null)))
                .thenReturn(new OAuthLoginResult(user, false));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication authentication = mock(Authentication.class);
        OAuth2User oauth2User = new DefaultOAuth2User(
                java.util.List.of(),
                Map.of("sub", "g1", "email", "r@example.com", "name", "Reader"),
                "sub"
        );
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        handler.onAuthenticationSuccess(request, response, authentication);

        assertEquals("/read/s/demo-series", response.getRedirectedUrl());
        assertEquals(
                "demo-series",
                session.getAttribute(AuthSessionKeys.PENDING_FOLLOW_SERIES_SLUG)
        );
        assertNull(session.getAttribute(AuthSessionKeys.OAUTH_RETURN_TO));
        verify(securityContextRepository).saveContext(any(), eq(request), eq(response));
    }

    @Test
    void pendingFollowDefaultsToSeriesHubWhenReturnToMissing() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthSessionKeys.OAUTH_INTENT, OAuthIntent.READER_LOGIN.name());
        session.setAttribute(AuthSessionKeys.PENDING_FOLLOW_SERIES_SLUG, "alpha");

        User user = User.create("g1", "r@example.com", "Reader", UserRole.READER);
        when(userOnboardingService.completeOAuthLogin(any(), eq(OAuthIntent.READER_LOGIN), eq(null)))
                .thenReturn(new OAuthLoginResult(user, false));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication authentication = mock(Authentication.class);
        OAuth2User oauth2User = new DefaultOAuth2User(
                java.util.List.of(),
                Map.of("sub", "g1", "email", "r@example.com", "name", "Reader"),
                "sub"
        );
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        handler.onAuthenticationSuccess(request, response, authentication);

        assertEquals("/read/s/alpha", response.getRedirectedUrl());
        assertEquals("alpha", session.getAttribute(AuthSessionKeys.PENDING_FOLLOW_SERIES_SLUG));
    }
}
