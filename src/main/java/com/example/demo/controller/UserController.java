package com.example.demo.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.User;
import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.LoginRequestDto;
import com.example.demo.dto.LoginResponseDto;
import com.example.demo.dto.RefreshTokenRequestDto;
import com.example.demo.dto.SignUpRequestDto;
import com.example.demo.dto.UserResponseDto;
import com.example.demo.email.EmailService;
import com.example.demo.jwt.JwtUtil;
import com.example.demo.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    public UserController(
            UserService userService,
            JwtUtil jwtUtil,
            EmailService emailService) {

        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
    }

    // 아이디 중복확인
    @GetMapping("/check-username")
    public ApiResponse<Map<String, Boolean>> checkUsername(
            @RequestParam("username") String username) {

        boolean available =
                userService.isUsernameAvailable(username);

        Map<String, Boolean> data = new HashMap<>();
        data.put("available", available);

        String message = available
                ? "사용 가능한 아이디입니다."
                : "이미 사용 중인 아이디입니다.";

        return ApiResponse.success(message, data);
    }

    // 회원가입
    @PostMapping("/signup")
    public ApiResponse<UserResponseDto> signup(
            @Valid @RequestBody SignUpRequestDto request) {

        if (!emailService.isVerified(request.getEmail())) {
            throw new RuntimeException(
                    "이메일 인증을 먼저 완료해주세요."
            );
        }

        User user = new User();

        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setEmail(
                request.getEmail()
                        .trim()
                        .toLowerCase()
        );

        User savedUser = userService.signup(user);

        emailService.clearVerification(
                request.getEmail()
        );

        UserResponseDto data =
                new UserResponseDto(
                        savedUser.getId(),
                        savedUser.getUsername(),
                        savedUser.getEmail()
                );

        return ApiResponse.success(
                "회원가입에 성공했습니다.",
                data
        );
    }

    // 로그인
    @PostMapping("/login")
    public ApiResponse<LoginResponseDto> login(
            @Valid @RequestBody LoginRequestDto request) {

        User user = userService.login(
                request.getUsername(),
                request.getPassword()
        );

        String accessToken =
                jwtUtil.generateAccessToken(
                        user.getUsername()
                );

        String refreshToken =
                jwtUtil.generateRefreshToken(
                        user.getUsername()
                );

        LoginResponseDto data =
                new LoginResponseDto(
                        accessToken,
                        refreshToken,
                        user.getId(),
                        user.getUsername(),
                        user.getEmail()
                );

        return ApiResponse.success(
                "로그인에 성공했습니다.",
                data
        );
    }

    // Access Token 재발급
    @PostMapping("/refresh")
    public ApiResponse<Map<String, String>> refresh(
            @Valid @RequestBody RefreshTokenRequestDto request) {

        String refreshToken =
                request.getRefreshToken();

        if (!jwtUtil.isRefreshToken(refreshToken)) {
            throw new RuntimeException(
                    "유효하지 않은 Refresh Token입니다."
            );
        }

        String username =
                jwtUtil.getUsername(refreshToken);

        String newAccessToken =
                jwtUtil.generateAccessToken(username);

        Map<String, String> data = new HashMap<>();
        data.put("token", newAccessToken);

        return ApiResponse.success(
                "Access Token이 재발급되었습니다.",
                data
        );
    }
}