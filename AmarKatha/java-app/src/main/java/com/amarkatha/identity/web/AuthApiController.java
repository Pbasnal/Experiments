package com.amarkatha.identity.web;

import com.amarkatha.identity.security.AmarKathaPrincipal;
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

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal AmarKathaPrincipal principal) {
        if (principal == null) {
            return Map.of("authenticated", false);
        }
        return Map.of(
                "authenticated", true,
                "email", principal.getEmail(),
                "displayName", principal.getDisplayName() == null ? "" : principal.getDisplayName(),
                "role", principal.getRole().name()
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return ResponseEntity.noContent().build();
    }
}
