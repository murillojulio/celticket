package com.celticket.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            // Deshabilitamos el control normal de rutas HTTP para permitir que WebSockets 
            // fluya libremente hasta el Interceptor de STOMP (donde realmente validamos el JWT).
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/seats-ws/**").permitAll()
                .anyRequest().permitAll()
            );

        return http.build();
    }
}
