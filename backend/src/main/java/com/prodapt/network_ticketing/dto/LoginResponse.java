package com.prodapt.network_ticketing.dto;

public class LoginResponse {
    private Long userId;
    private String username;
    private String role;
    private String token;

    // constructor + getters


    public LoginResponse(Long userId, String username, String role, String token) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.token = token;
    }

    public LoginResponse() {
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public String getToken() {
        return token;
    }
}