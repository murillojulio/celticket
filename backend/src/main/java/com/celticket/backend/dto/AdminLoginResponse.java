package com.celticket.backend.dto;

public class AdminLoginResponse {

    private String token;
    private long expiresAt;

    public AdminLoginResponse(String token, long expiresAt) {
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public long getExpiresAt() {
        return expiresAt;
    }
}
