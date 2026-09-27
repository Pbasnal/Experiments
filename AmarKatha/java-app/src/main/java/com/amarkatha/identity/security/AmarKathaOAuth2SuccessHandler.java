package com.amarkatha.identity.security;

import com.amarkatha.identity.OAuthIntent;
import com.amarkatha.identity.OAuthLoginResult;
import com.amarkatha.identity.OAuthOnboardingException;
import com.amarkatha.identity.UserOnboardingService;
import com.amarkatha.identity.domain.User;
import com.amarkatha.shared.domain.UserRole;
import com.amarkatha.shared.events.ReaderSignedUpTelemetry;
import com.amarkatha.shared.web.ReaderIdCookie;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

@Component
public class AmarKathaOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserOnboardingService userOnboardingService;
    private final SecurityContextRepository securityContextRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AmarKathaOAuth2SuccessHandler(
            UserOnboardingService userOnboardingService,
            SecurityContextRepository securityContextRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.userOnboardingService = userOnboardingService;
        this.securityContextRepository = securityContextRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        OAuthIntent intent = readIntent(session);
        String pendingInvite = readPendingInvite(session);
        String returnTo = SafeReturnPath.peek(session).orElse(null);
        String pendingFollow = readPendingFollow(session);

        try {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            OAuthLoginResult login = userOnboardingService.completeOAuthLogin(oauth2User, intent, pendingInvite);
            User user = login.user();
            // Keep returnTo / pending follow for post-login redirect; clear invite/intent only.
            clearSignupSession(session);
            Authentication updated = new AmarKathaAuthenticationToken(
                    new AmarKathaPrincipal(user, oauth2User.getAttributes()),
                    oauth2User.getAttributes()
            );
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(updated);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);
            if (login.newAccount() && user.getRole() == UserRole.READER) {
                eventPublisher.publishEvent(new ReaderSignedUpTelemetry(
                        ReaderIdCookie.ensure(request, response)
                ));
            }
            response.sendRedirect(redirectFor(user, returnTo, pendingFollow, session));
        } catch (OAuthOnboardingException ex) {
            clearSignupSession(session);
            clearReturnSession(session);
            response.sendRedirect(failureRedirect(ex.getReason()));
        }
    }

    private static OAuthIntent readIntent(HttpSession session) {
        if (session == null) {
            return OAuthIntent.CREATOR_LOGIN;
        }
        Object raw = session.getAttribute(AuthSessionKeys.OAUTH_INTENT);
        if (raw == null) {
            return OAuthIntent.CREATOR_LOGIN;
        }
        try {
            return OAuthIntent.valueOf(raw.toString());
        } catch (IllegalArgumentException ex) {
            return OAuthIntent.CREATOR_LOGIN;
        }
    }

    private static String readPendingInvite(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object raw = session.getAttribute(AuthSessionKeys.PENDING_INVITE_TOKEN);
        return raw == null ? null : raw.toString();
    }

    private static String readPendingFollow(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object raw = session.getAttribute(AuthSessionKeys.PENDING_FOLLOW_SERIES_SLUG);
        if (raw == null) {
            return null;
        }
        return SafeReturnPath.normalizeSeriesSlug(raw.toString()).orElse(null);
    }

    private static void clearSignupSession(HttpSession session) {
        if (session == null) {
            return;
        }
        session.removeAttribute(AuthSessionKeys.OAUTH_INTENT);
        session.removeAttribute(AuthSessionKeys.PENDING_INVITE_TOKEN);
    }

    private static void clearReturnSession(HttpSession session) {
        if (session == null) {
            return;
        }
        session.removeAttribute(AuthSessionKeys.OAUTH_RETURN_TO);
        session.removeAttribute(AuthSessionKeys.PENDING_FOLLOW_SERIES_SLUG);
    }

    private static String redirectFor(
            User user,
            String returnTo,
            String pendingFollow,
            HttpSession session
    ) {
        if (pendingFollow != null) {
            // Return safely to the SPA; keep pending follow in session for an authenticated POST.
            if (session != null) {
                session.removeAttribute(AuthSessionKeys.OAUTH_RETURN_TO);
            }
            if (returnTo != null) {
                return returnTo;
            }
            return "/read/s/" + pendingFollow;
        }
        if (returnTo != null) {
            if (session != null) {
                session.removeAttribute(AuthSessionKeys.OAUTH_RETURN_TO);
                session.removeAttribute(AuthSessionKeys.PENDING_FOLLOW_SERIES_SLUG);
            }
            return returnTo;
        }
        if (session != null) {
            session.removeAttribute(AuthSessionKeys.PENDING_FOLLOW_SERIES_SLUG);
        }
        return switch (user.getRole()) {
            case ADMIN -> "/admin";
            case CREATOR -> "/creator";
            case READER -> "/";
        };
    }

    private static String failureRedirect(OAuthOnboardingException.Reason reason) {
        return switch (reason) {
            case INVITE_REQUIRED -> "/creator/signup?error=invite_required";
            case INVITE_EXPIRED -> "/creator/signup?error=invite_expired";
            case INVITE_EXHAUSTED -> "/creator/signup?error=invite_exhausted";
            case INVITE_INVALID -> "/creator/signup?error=invite_not_found";
            case ACCOUNT_NOT_FOUND -> "/login?error=account_not_found";
            case ADMIN_ACCESS_DENIED -> "/login?error=admin_denied";
            case MISSING_PROFILE -> "/login?error=oauth_failed";
        };
    }
}
