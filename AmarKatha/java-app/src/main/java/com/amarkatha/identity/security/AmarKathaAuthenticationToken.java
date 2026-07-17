package com.amarkatha.identity.security;

import java.io.Serial;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class AmarKathaAuthenticationToken extends OAuth2AuthenticationToken {

    @Serial
    private static final long serialVersionUID = 1L;

    public AmarKathaAuthenticationToken(AmarKathaPrincipal principal, Map<String, Object> attributes) {
        super(principal, principal.getAuthorities(), "google");
        setDetails(attributes);
    }

    @Override
    public AmarKathaPrincipal getPrincipal() {
        return (AmarKathaPrincipal) super.getPrincipal();
    }

    public static AmarKathaPrincipal requirePrincipal(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("Not authenticated");
        }
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        if (principal instanceof AmarKathaPrincipal amarKathaPrincipal) {
            return amarKathaPrincipal;
        }
        throw new IllegalStateException("Unexpected principal type: " + principal.getClass().getName());
    }
}
