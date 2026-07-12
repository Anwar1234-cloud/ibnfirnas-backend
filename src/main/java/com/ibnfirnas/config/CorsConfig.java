package com.ibnfirnas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        //  false — wildcard * ke saath credentials true nahi ho sakta
        config.setAllowCredentials(false);

        // All origins allow — ngrok, localhost, mobile app
        config.setAllowedOriginPatterns(List.of("*"));

        //  All headers allow
        config.setAllowedHeaders(List.of("*"));

        //  All methods allow + PATCH bhi
        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        //  Headers jo response mein expose hon
        config.setExposedHeaders(List.of(
                "Authorization",
                "Content-Type"
        ));

        //  Preflight cache — 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}