package com.example.demo.dto;

public class LoginResponseDto {

    // Access Token
    // 기존 프론트와 호환하기 위해 이름은 token 그대로 사용
    private String token;

    // Refresh Token
    private String refreshToken;

    private Long id;
    private String username;
    private String email;

    public LoginResponseDto(
            String token,
            String refreshToken,
            Long id,
            String username,
            String email) {

        this.token = token;
        this.refreshToken = refreshToken;
        this.id = id;
        this.username = username;
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    public String getRefreshToken() {
        return refreshToken;
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