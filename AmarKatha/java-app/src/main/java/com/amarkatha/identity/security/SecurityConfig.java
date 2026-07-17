package com.amarkatha.identity.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

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
                        .requestMatchers(
                                "/",
                                "/read/**",
                                "/api/reader/**",
                                "/actuator/health",
                                "/actuator/info",
                                "/error",
                                "/creator/signup",
                                "/creator/signup/**",
                                "/creator/login",
                                "/admin/login",
                                "/oauth2/**",
                                "/login/oauth2/**"
                        ).permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/creator/**").hasAnyRole("CREATOR", "ADMIN")
                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/creator/login")
                        .successHandler(successHandler)
                )
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/creator/login"),
                                request -> request.getRequestURI().startsWith("/admin"))
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/creator/login"))
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/reader/**"))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
                .addFilterBefore(oauthInviteGateFilter, org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter.class);
        return http.build();
    }
}
