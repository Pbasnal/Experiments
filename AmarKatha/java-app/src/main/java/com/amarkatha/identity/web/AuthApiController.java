package com.amarkatha.identity.web;

import com.amarkatha.identity.UserRepository;
import com.amarkatha.identity.domain.User;
import com.amarkatha.identity.security.AmarKathaPrincipal;
import com.amarkatha.shared.domain.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final UserRepository userRepository;

    public AuthApiController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal AmarKathaPrincipal principal) {
        if (principal == null) {
            return Map.of("authenticated", false);
        }
        boolean demoMode = userRepository.findById(principal.getId())
                .map(AuthApiController::effectiveDemoMode)
                .orElse(false);
        return Map.of(
                "authenticated", true,
                "email", principal.getEmail(),
                "displayName", principal.getDisplayName() == null ? "" : principal.getDisplayName(),
                "role", principal.getRole().name(),
                "demoMode", demoMode
        );
    }

    /**
     * Mirrors {@code ExperienceSourcePolicy}: demo content only for an admin who turned the preview on.
     */
    private static boolean effectiveDemoMode(User user) {
        return user.getRole() == UserRole.ADMIN && user.isDemoMode();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return ResponseEntity.noContent().build();
    }
}
