package com.amarkatha.identity.security;

import com.amarkatha.identity.domain.User;
import com.amarkatha.shared.domain.UserRole;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Session-safe principal: plain fields only (no JPA entity) so Spring Session JDBC can serialize it.
 */
public class AmarKathaPrincipal implements OAuth2User, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String googleSub;
    private final String email;
    private final String displayName;
    private final UserRole role;
    private final Map<String, Object> attributes;

    public AmarKathaPrincipal(User user, Map<String, Object> attributes) {
        this.id = user.getId();
        this.googleSub = user.getGoogleSub();
        this.email = user.getEmail();
        this.displayName = user.getDisplayName();
        this.role = user.getRole();
        this.attributes = attributes == null
                ? Map.of()
                : Collections.unmodifiableMap(new HashMap<>(attributes));
    }

    public UUID getId() {
        return id;
    }

    public String getGoogleSub() {
        return googleSub;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UserRole getRole() {
        return role;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(roleAuthority(role)));
    }

    @Override
    public String getName() {
        return googleSub;
    }

    public static String roleAuthority(UserRole role) {
        return "ROLE_" + role.name();
    }
}
