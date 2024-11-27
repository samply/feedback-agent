package com.samply.feedbackagent.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {
    private static final Logger logger = LogManager.getLogger(CorsConfig.class);

    // Load the CORS origin dynamically, e.g., from environment variables or properties
    @Value("${fa.cors.origin}")
    String corsOrigin;

    @Bean
    public CorsFilter corsFilter() {
        logger.info("corsFilter: corsOrigin: " + corsOrigin);
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin(corsOrigin); // Set your dynamic origin
        config.addAllowedHeader("*"); // Allow all headers
        config.addAllowedMethod("*"); // Allow all HTTP methods
        config.setAllowCredentials(true); // Allow cookies if necessary

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
