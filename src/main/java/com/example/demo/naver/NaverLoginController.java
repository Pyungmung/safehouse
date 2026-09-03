package com.example.demo.naver;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.User;
import com.example.demo.email.EmailService;
import com.example.demo.jwt.JwtUtil;
import com.example.demo.service.UserService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/naver")
public class NaverLoginController {

    private final NaverLoginService naverLoginService;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    public NaverLoginController(
            NaverLoginService naverLoginService,
            UserService userService,
            JwtUtil jwtUtil,
            EmailService emailService) {

        this.naverLoginService = naverLoginService;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
    }

    // 네이버 로그인 시작
    @GetMapping("/login")
    public ResponseEntity<Void> login(
            HttpSession session) {

        String state =
                UUID.randomUUID().toString();

        // 우리가 만든 state를 세션에 저장
        session.setAttribute(
                "naverState",
                state
        );

        String loginUrl =
                naverLoginService.getLoginUrl(state);

        HttpHeaders headers =
                new HttpHeaders();

        headers.setLocation(
                URI.create(loginUrl)
        );

        return new ResponseEntity<>(
                headers,
                HttpStatus.FOUND
        );
    }

    // 네이버 로그인 완료 후
    @GetMapping("/callback")
    public ResponseEntity<String> callback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpSession session) {

        // 1. 우리가 저장했던 state 가져오기
        Object savedStateObject =
                session.getAttribute(
                        "naverState"
                );

        // 한 번 사용했으면 삭제
        session.removeAttribute(
                "naverState"
        );

        if (savedStateObject == null) {
            throw new RuntimeException(
                    "잘못된 네이버 로그인 요청입니다."
            );
        }

        String savedState =
                savedStateObject.toString();

        // 네이버가 돌려준 state와 비교
        if (!savedState.equals(state)) {
            throw new RuntimeException(
                    "네이버 로그인 보안 검증에 실패했습니다."
            );
        }

        // 2. 네이버 Access Token 받기
        String accessToken =
                naverLoginService.getAccessToken(
                        code,
                        state
                );

        // 3. 네이버 사용자 정보 받기
        Map<String, Object> userInfo =
                naverLoginService.getUserInfo(
                        accessToken
                );

        if (userInfo.get("id") == null) {
            throw new RuntimeException(
                    "네이버 사용자 ID를 가져오지 못했습니다."
            );
        }

        if (userInfo.get("email") == null) {
            throw new RuntimeException(
                    "네이버 이메일 제공 동의가 필요합니다."
            );
        }

        String naverId =
                userInfo.get("id")
                        .toString();

        String email =
                userInfo.get("email")
                        .toString()
                        .trim()
                        .toLowerCase();

        // 4. 회원 조회 또는 자동 회원가입
        User user =
                userService.naverLogin(
                        naverId,
                        email
                );

        // 5. JWT 발급
        String token =
                jwtUtil.generateToken(
                        user.getUsername()
                );

        // 6. 로그인 알림 이메일
        try {

            emailService
                    .sendNaverLoginNotification(
                            user.getEmail()
                    );

        } catch (Exception e) {

            System.out.println(
                    "네이버 로그인 알림 이메일 전송 실패: "
                            + e.getMessage()
            );
        }

        // 7. JWT 저장 후 메인화면으로 이동
        String html =
                """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <title>SafeHouse 로그인</title>
                </head>

                <body>

                <script>

                    localStorage.setItem(
                        "token",
                        "%s"
                    );

                    localStorage.setItem(
                        "username",
                        "%s"
                    );

                    localStorage.setItem(
                        "naverLoginSuccess",
                        "true"
                    );

                    window.location.href = "/";

                </script>

                </body>
                </html>
                """.formatted(
                        token,
                        user.getUsername()
                );

        return ResponseEntity
                .ok()
                .header(
                        "Content-Type",
                        "text/html; charset=UTF-8"
                )
                .body(html);
    }
}