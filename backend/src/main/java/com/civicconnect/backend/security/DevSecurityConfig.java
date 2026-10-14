package com.civicconnect.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * TEMPORARY, dev profile only.
 * Spring Security locks every endpoint by default, which would make the API look
 * "broken" while you are still wiring up the database. This opens /api/** for local
 * development ONLY. Replace it with the real RBAC configuration (login + role checks)
 * before any milestone release candidate. It is not active in any other profile.
 */
@Configuration
@Profile("dev")
public class DevSecurityConfig {

    @Bean
    SecurityFilterChain devFilterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
