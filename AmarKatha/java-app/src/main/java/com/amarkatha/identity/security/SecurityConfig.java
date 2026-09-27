package com.amarkatha.identity.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] ENGAGEMENT_ROLES = {"READER", "CREATOR", "ADMIN"};

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            OAuthInviteGateFilter oauthInviteGateFilter,
            AmarKathaOAuth2SuccessHandler successHandler,
            SecurityContextRepository securityContextRepository
    ) throws Exception {
        http
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .authorizeHttpRequests(auth -> auth
                        // Engagement mutations + portal (public GET catalog/events stay anonymous below)
                        .requestMatchers("/api/reader/v1/me", "/api/reader/v1/me/**")
                        .hasAnyRole(ENGAGEMENT_ROLES)
                        .requestMatchers(HttpMethod.POST, "/api/reader/v1/series/*/follow")
                        .hasAnyRole(ENGAGEMENT_ROLES)
                        .requestMatchers(HttpMethod.DELETE, "/api/reader/v1/series/*/follow")
                        .hasAnyRole(ENGAGEMENT_ROLES)
                        .requestMatchers(
                                "/",
                                "/read/**",
                                "/legal/**",
                                "/media/**",
                                "/api/reader/**",
                                "/api/auth/**",
                                "/actuator/health",
                                "/actuator/info",
                                "/actuator/prometheus",
                                "/error",
                                "/login",
                                "/login/**",
                                "/creator/signup",
                                "/creator/signup/**",
                                "/creator/login",
                                "/admin/login",
                                "/oauth2/**",
                                "/login/oauth2/**"
                        ).permitAll()
                        .requestMatchers("/profile").authenticated()
                        .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/creator/**").hasAnyRole("CREATOR", "ADMIN")
                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .successHandler(successHandler)
                )
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                new AntPathRequestMatcher("/api/**"))
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                request -> request.getRequestURI().startsWith("/admin"))
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/reader/**", "/api/auth/**", "/api/admin/**"))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("SESSION", "JSESSIONID")
                )
                .addFilterBefore(oauthInviteGateFilter, org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter.class);
        return http.build();
    }
}
