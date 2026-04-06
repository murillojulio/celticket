package com.celticket.backend.config;

import com.celticket.backend.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    @Autowired
    private JwtService jwtService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/seats-ws")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                // Evita cookie JSESSIONID en SockJS; sin esto el navegador manda credenciales en el
                // XHR a /seats-ws/info y CORS exige Access-Control-Allow-Credentials: true (incompatible con *).
                .setSessionCookieNeeded(false);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    List<String> authorization = accessor.getNativeHeader("Authorization");
                    logger.debug("STOMP Connect requested. Auth Header: {}", authorization);

                    if (authorization != null && !authorization.isEmpty()) {
                        String token = authorization.get(0).replace("Bearer ", "");

                        if (jwtService.isTokenValid(token)) {
                            String username = jwtService.extractUsername(token);
                            Authentication userAuth = new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());
                            accessor.setUser(userAuth);
                            logger.info("WebSocket Authenticated successfully for user: {}", username);
                        } else {
                            logger.warn("Invalid JWT token detected during WebSocket Handshake");
                            throw new IllegalArgumentException("Token JWT inválido");
                        }
                    } else {
                        // En producción podrías querer lanzar esta excepción. Para la prueba local (si no envían token de momento) 
                        // puedes relajar esto temporalmente o generar un usuario anónimo falso. Para estricto:
                        // throw new IllegalArgumentException("No se proporcionó Header de Autorización");
                        logger.warn("No Auth Header provided. Proceeding as Anonymous (if allowed by rules)");
                    }
                }
                return message;
            }
        });
    }
}
