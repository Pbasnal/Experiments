package com.amarkatha.identity.domain;

import com.amarkatha.shared.domain.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class User {

    @Id
    private UUID id;

    @Column(name = "google_sub", nullable = false, unique = true)
    private String googleSub;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "display_name")
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.READER;

    @Column(nullable = false)
    private String locale = "en";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected User() {
    }

    public static User create(String googleSub, String email, String displayName, UserRole role) {
        User user = new User();
        user.id = UUID.randomUUID();
        user.googleSub = googleSub;
        user.email = email;
        user.displayName = displayName;
        user.role = role;
        return user;
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

    public String getLocale() {
        return locale;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }
}
