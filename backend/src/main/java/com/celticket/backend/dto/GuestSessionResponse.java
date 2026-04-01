package com.celticket.backend.dto;

public class GuestSessionResponse {
    private String sessionId;
    private String token;
    private Long expiresAtEpochMs;

    public GuestSessionResponse() {
    }

    public GuestSessionResponse(String sessionId, String token, Long expiresAtEpochMs) {
        this.sessionId = sessionId;
        this.token = token;
        this.expiresAtEpochMs = expiresAtEpochMs;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getExpiresAtEpochMs() {
        return expiresAtEpochMs;
    }

    public void setExpiresAtEpochMs(Long expiresAtEpochMs) {
        this.expiresAtEpochMs = expiresAtEpochMs;
    }
}
