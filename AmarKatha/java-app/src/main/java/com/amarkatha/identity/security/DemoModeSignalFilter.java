package com.amarkatha.identity.security;

import com.amarkatha.identity.UserRepository;
import com.amarkatha.identity.domain.User;
import com.amarkatha.shared.demo.DemoModeSignals;
import com.amarkatha.shared.domain.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Publishes the signed-in account's admin and demo-mode facts for this request.
 * Registered inside the security chain so the session principal is already loaded.
 */
public class DemoModeSignalFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public DemoModeSignalFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AmarKathaPrincipal principal) {
            request.setAttribute(DemoModeSignals.VIEWER_ID, principal.getId());
            userRepository.findById(principal.getId()).ifPresent(user -> mark(request, user));
        }
        filterChain.doFilter(request, response);
    }

    private static void mark(HttpServletRequest request, User user) {
        request.setAttribute(DemoModeSignals.ADMIN, user.getRole() == UserRole.ADMIN);
        request.setAttribute(DemoModeSignals.REQUESTED, user.isDemoMode());
    }
}
