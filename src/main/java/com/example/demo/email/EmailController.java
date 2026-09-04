package com.example.demo.email;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;

@RestController
@RequestMapping("/api/email")
public class EmailController {

    private final EmailService emailService;

    public EmailController(
            EmailService emailService) {

        this.emailService = emailService;
    }

    // 인증번호 이메일 전송
    @PostMapping("/send")
    public ApiResponse<Void> sendCode(
            @RequestBody Map<String, String> request) {

        String email = request.get("email");

        emailService.sendVerificationCode(email);

        return ApiResponse.success(
                "인증번호를 전송했습니다."
        );
    }

    // 인증번호 확인
    @PostMapping("/verify")
    public ApiResponse<Map<String, Boolean>> verifyCode(
            @RequestBody Map<String, String> request) {

        String email = request.get("email");
        String code = request.get("code");

        boolean success =
                emailService.verifyCode(
                        email,
                        code
                );

        Map<String, Boolean> data =
                new HashMap<>();

        data.put("verified", success);

        if (success) {

            return ApiResponse.success(
                    "이메일 인증에 성공했습니다.",
                    data
            );
        }

        return new ApiResponse<>(
                false,
                "인증번호가 틀렸거나 만료되었습니다.",
                data
        );
    }
}