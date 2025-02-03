package com.samply.feedbackagent;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    /**
     * Configures the security filter chain for the application.
     *
     * This configuration disables CSRF protection and allows all HTTP requests
     * to be accessed without authentication.
     *
     * @param http the HttpSecurity to modify
     * @return the SecurityFilterChain that permits all requests
     * @throws Exception if an error occurs while building the security filter chain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())  // ❌ Disable CSRF protection
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/**").permitAll() // Allow all requests
            )
            .build();
    }
}
