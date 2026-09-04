package com.example.demo.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    // Access Token: 1시간
    private static final long ACCESS_TOKEN_EXPIRATION =
            60 * 60 * 1000;

    // Refresh Token: 7일
    private static final long REFRESH_TOKEN_EXPIRATION =
            7 * 24 * 60 * 60 * 1000;

    private final SecretKey key;

    public JwtUtil(
            @Value("${jwt.secret}") String secretKey) {

        this.key = Keys.hmacShaKeyFor(
                secretKey.getBytes(StandardCharsets.UTF_8)
        );
    }

    // 기존 코드와의 호환성을 위해 유지
    public String generateToken(String username) {
        return generateAccessToken(username);
    }

    // Access Token 생성
    public String generateAccessToken(String username) {

        return createToken(
                username,
                "access",
                ACCESS_TOKEN_EXPIRATION
        );
    }

    // Refresh Token 생성
    public String generateRefreshToken(String username) {

        return createToken(
                username,
                "refresh",
                REFRESH_TOKEN_EXPIRATION
        );
    }

    // 공통 토큰 생성
    private String createToken(
            String username,
            String type,
            long expirationTime) {

        Date now = new Date();
        Date expiration =
                new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .subject(username)
                .claim("type", type)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    // JWT에서 username 가져오기
    public String getUsername(String token) {

        return getClaims(token).getSubject();
    }

    // 토큰 종류 가져오기
    public String getTokenType(String token) {

        return getClaims(token)
                .get("type", String.class);
    }

    // JWT 검증
    public boolean validateToken(String token) {

        try {
            getClaims(token);
            return true;

        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // Access Token인지 확인
    public boolean isAccessToken(String token) {

        return validateToken(token)
                && "access".equals(getTokenType(token));
    }

    // Refresh Token인지 확인
    public boolean isRefreshToken(String token) {

        return validateToken(token)
                && "refresh".equals(getTokenType(token));
    }

    // JWT 내용 읽기
    private Claims getClaims(String token) {

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}