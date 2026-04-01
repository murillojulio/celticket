package com.celticket.backend.controller;

import com.celticket.backend.dto.GuestSessionRequest;
import com.celticket.backend.dto.GuestSessionResponse;
import com.celticket.backend.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/guest-session")
    public ResponseEntity<?> createGuestSession(@RequestBody(required = false) GuestSessionRequest request) {
        String incomingSessionId = request == null ? null : request.getSessionId();
        String sessionId;
        if (StringUtils.hasText(incomingSessionId)) {
            sessionId = incomingSessionId == null ? generateSessionId() : incomingSessionId.trim();
        } else {
            sessionId = generateSessionId();
        }
        if (!isSafeSessionId(sessionId)) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "sessionId invalido."));
        }

        String token = jwtService.generateToken("session:" + sessionId);
        long expiresAt = jwtService.extractExpirationDate(token).getTime();

        return ResponseEntity.ok(new GuestSessionResponse(sessionId, token, expiresAt));
    }

    private String generateSessionId() {
        return "sess_" + UUID.randomUUID().toString().replace("-", "");
    }

    private boolean isSafeSessionId(String sessionId) {
        return sessionId.length() <= 128 && sessionId.matches("^[a-zA-Z0-9._:-]+$");
    }
}
