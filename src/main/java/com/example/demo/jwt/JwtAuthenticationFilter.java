package com.example.demo.jwt;

import java.io.IOException;
import java.util.Collections;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // Authorization 헤더 가져오기
        String authorizationHeader =
                request.getHeader("Authorization");

        // Bearer 토큰이 있는지 확인
        if (authorizationHeader != null
                && authorizationHeader.startsWith("Bearer ")) {

            // "Bearer " 제거
            String token =
                    authorizationHeader.substring(7);

            // Access Token일 때만 인증 처리
            if (jwtUtil.isAccessToken(token)) {

                // 토큰에서 사용자 아이디 가져오기
                String username =
                        jwtUtil.getUsername(token);

                // Spring Security 인증 객체 생성
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                Collections.emptyList()
                        );

                // 인증된 사용자로 등록
                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);
            }
        }

        // 다음 필터로 이동
        filterChain.doFilter(request, response);
    }
}