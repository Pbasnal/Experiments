package com.amarkatha.identity.security;

import com.amarkatha.identity.domain.User;
import com.amarkatha.shared.domain.UserRole;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class AmarKathaPrincipal implements OAuth2User {

    private final User user;
    private final Map<String, Object> attributes;

    public AmarKathaPrincipal(User user, Map<String, Object> attributes) {
        this.user = user;
        this.attributes = attributes;
    }

    public User getUser() {
        return user;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(roleAuthority(user.getRole())));
    }

    @Override
    public String getName() {
        return user.getGoogleSub();
    }

    public static String roleAuthority(UserRole role) {
        return "ROLE_" + role.name();
    }
}
