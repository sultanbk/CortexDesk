package com.prodapt.network_ticketing.dto;

public class LoginRequest {

    private String username;
    private String password; // optional for now

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
