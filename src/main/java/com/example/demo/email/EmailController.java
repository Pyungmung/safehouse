package com.example.demo.email;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/email")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    // 인증번호 이메일 전송
    @PostMapping("/send")
    public Map<String, String> sendCode(
            @RequestBody Map<String, String> request) {

        String email = request.get("email");

        emailService.sendVerificationCode(email);

        Map<String, String> response = new HashMap<>();
        response.put("message", "인증번호를 전송했습니다.");

        return response;
    }

    // 인증번호 확인
    @PostMapping("/verify")
    public Map<String, String> verifyCode(
            @RequestBody Map<String, String> request) {

        String email = request.get("email");
        String code = request.get("code");

        boolean success =
                emailService.verifyCode(email, code);

        Map<String, String> response = new HashMap<>();

        if (success) {
            response.put("message", "이메일 인증에 성공했습니다.");
        } else {
            response.put("message", "인증번호가 틀렸거나 만료되었습니다.");
        }

        return response;
    }
}