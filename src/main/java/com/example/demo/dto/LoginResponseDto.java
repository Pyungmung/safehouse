package com.example.demo.dto;

public class LoginResponseDto {

    private String token;
    private Long id;
    private String username;
    private String email;

    public LoginResponseDto(
            String token,
            Long id,
            String username,
            String email) {

        this.token = token;
        this.id = id;
        this.username = username;
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }
}