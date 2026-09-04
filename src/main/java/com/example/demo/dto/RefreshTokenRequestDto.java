package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

public class RefreshTokenRequestDto {

    @NotBlank(message = "Refresh Token을 입력해주세요.")
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}