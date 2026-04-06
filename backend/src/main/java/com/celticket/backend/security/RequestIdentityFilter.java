package com.celticket.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Component
public class RequestIdentityFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String SESSION_HEADER = "X-Session-Id";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final boolean allowUnsignedSessionId;

    public RequestIdentityFilter(JwtService jwtService,
                                 @Value("${security.allow-unsigned-session-id:true}") boolean allowUnsignedSessionId) {
        this.jwtService = jwtService;
        this.allowUnsignedSessionId = allowUnsignedSessionId;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String authHeader = request.getHeader(AUTHORIZATION_HEADER);
            String sessionId = request.getHeader(SESSION_HEADER);

            if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
                String token = authHeader.substring(BEARER_PREFIX.length()).trim();
                if (!jwtService.isTokenValid(token)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Token JWT invalido.");
                    return;
                }

                String username = jwtService.extractUsername(token);
                if (StringUtils.hasText(username)) {
                    List<GrantedAuthority> authorities = username.startsWith("admin:")
                            ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                            : Collections.emptyList();
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(username, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } else if (allowUnsignedSessionId && StringUtils.hasText(sessionId)) {
                String identity = "session:" + sessionId.trim();
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(identity, null, Collections.emptyList());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
