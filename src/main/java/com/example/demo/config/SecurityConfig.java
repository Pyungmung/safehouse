package com.example.demo.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.demo.jwt.JwtAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter) {

        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            // CORS 설정 적용
            .cors(cors ->
                cors.configurationSource(
                    corsConfigurationSource()
                )
            )

            // JWT 방식이므로 CSRF 비활성화
            .csrf(csrf -> csrf.disable())

            // 기본 로그인 화면 비활성화
            .formLogin(form -> form.disable())

            // HTTP Basic 인증 비활성화
            .httpBasic(basic -> basic.disable())

            // 세션 대신 JWT 사용
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            // API 접근 권한
            .authorizeHttpRequests(auth -> auth

                // 메인 화면
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/favicon.ico",
                    "/error"
                ).permitAll()

                // 회원 관련 공개 API
                .requestMatchers(
                    "/api/users/signup",
                    "/api/users/login",
                    "/api/users/check-username",
                    "/api/users/refresh"
                ).permitAll()

                // 이메일 인증
                .requestMatchers(
                    "/api/email/**"
                ).permitAll()

                // 네이버 로그인
                .requestMatchers(
                    "/api/naver/**"
                ).permitAll()

                // 나머지는 JWT 인증 필요
                .anyRequest().authenticated()
            )

            // JWT 인증 필터 연결
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    // CORS 전역 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        // 개발 중 프론트엔드 주소 허용
        configuration.setAllowedOrigins(
                List.of(
                    "http://localhost:3000",
                    "http://localhost:5173",
                    "http://localhost:8080",
                    "http://safehouse:8080"
                )
        );

        // 허용 HTTP 메서드
        configuration.setAllowedMethods(
                List.of(
                    "GET",
                    "POST",
                    "PUT",
                    "PATCH",
                    "DELETE",
                    "OPTIONS"
                )
        );

        // 모든 요청 헤더 허용
        configuration.setAllowedHeaders(
                List.of("*")
        );

        // Authorization 등의 응답 헤더 접근 허용
        configuration.setExposedHeaders(
                List.of("Authorization")
        );

        // 쿠키/인증정보 전송 허용
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        // 모든 API에 CORS 적용
        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}