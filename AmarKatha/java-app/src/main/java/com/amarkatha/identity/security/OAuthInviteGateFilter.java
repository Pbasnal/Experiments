package com.amarkatha.identity.security;

import com.amarkatha.identity.OAuthIntent;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Blocks Google OAuth until a login/signup intent is set in session.
 * Invite is validated later in {@link com.amarkatha.identity.UserOnboardingService} for new creators.
 */
@Component
public class OAuthInviteGateFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!isGoogleOAuthStart(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
                && !(SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof String)) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        if (session != null && hasAllowedOAuthIntent(session)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.sendRedirect("/creator/signup?error=invite_required");
    }

    private static boolean isGoogleOAuthStart(HttpServletRequest request) {
        return "GET".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().equals("/oauth2/authorization/google");
    }

    private static boolean hasAllowedOAuthIntent(HttpSession session) {
        Object intent = session.getAttribute(AuthSessionKeys.OAUTH_INTENT);
        if (intent == null) {
            return false;
        }
        String value = intent.toString();
        return OAuthIntent.GOOGLE_AUTH.name().equals(value)
                || OAuthIntent.CREATOR_LOGIN.name().equals(value)
                || OAuthIntent.ADMIN_LOGIN.name().equals(value)
                || OAuthIntent.CREATOR_SIGNUP.name().equals(value);
    }
}
