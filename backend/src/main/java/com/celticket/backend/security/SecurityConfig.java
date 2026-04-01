package com.celticket.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final RequestIdentityFilter requestIdentityFilter;

    public SecurityConfig(RequestIdentityFilter requestIdentityFilter) {
        this.requestIdentityFilter = requestIdentityFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/seats-ws/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/guest-session").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/events/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/payments/wompi/webhook").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/checkout").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/payments/wompi/quote").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/payments/wompi/checkout-session").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/payments/wompi/orders/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/events/*/layout").authenticated()
                .anyRequest().denyAll()
            )
            .addFilterBefore(requestIdentityFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
