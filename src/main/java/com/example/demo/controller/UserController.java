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
import com.example.demo.dto.LoginRequestDto;
import com.example.demo.dto.LoginResponseDto;
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

    // 아이디 중복 확인
    @GetMapping("/check-username")
    public Map<String, Object> checkUsername(
            @RequestParam("username") String username) {

        boolean available =
                userService.isUsernameAvailable(username);

        Map<String, Object> response =
                new HashMap<>();

        response.put("available", available);

        if (available) {

            response.put(
                    "message",
                    "사용 가능한 아이디입니다."
            );

        } else {

            response.put(
                    "message",
                    "이미 사용 중인 아이디입니다."
            );

        }

        return response;
    }

    // 회원가입
    @PostMapping("/signup")
    public UserResponseDto signup(
            @Valid @RequestBody SignUpRequestDto request) {

        // 이메일 인증 여부 확인
        if (!emailService.isVerified(request.getEmail())) {

            throw new RuntimeException(
                    "이메일 인증을 먼저 완료해주세요."
            );

        }

        User user = new User();

        user.setUsername(
                request.getUsername()
        );

        user.setPassword(
                request.getPassword()
        );

        user.setEmail(
                request.getEmail()
                        .trim()
                        .toLowerCase()
        );

        User savedUser =
                userService.signup(user);

        // 회원가입 성공 후 이메일 인증 상태 삭제
        emailService.clearVerification(
                request.getEmail()
        );

        return new UserResponseDto(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail()
        );
    }

    // 로그인
    @PostMapping("/login")
    public LoginResponseDto login(
            @Valid @RequestBody LoginRequestDto request) {

        User user =
                userService.login(
                        request.getUsername(),
                        request.getPassword()
                );

        String token =
                jwtUtil.generateToken(
                        user.getUsername()
                );

        return new LoginResponseDto(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail()
        );
    }

}