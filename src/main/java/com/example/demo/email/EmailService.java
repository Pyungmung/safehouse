package com.example.demo.email;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderUsername;

    private final SecureRandom random =
            new SecureRandom();

    // 이메일별 인증번호 저장
    private final Map<String, String> verificationCodes =
            new ConcurrentHashMap<>();

    // 이메일별 인증번호 만료시간 저장
    private final Map<String, Long> expirationTimes =
            new ConcurrentHashMap<>();

    // 인증 완료된 이메일 저장
    private final Set<String> verifiedEmails =
            ConcurrentHashMap.newKeySet();

    // 인증번호 유효시간 5분
    private static final long EXPIRATION_TIME =
            5 * 60 * 1000;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // 6자리 인증번호 생성
    public String createVerificationCode() {

        int number =
                100000 + random.nextInt(900000);

        return String.valueOf(number);
    }

    // 인증번호 이메일 전송
    public void sendVerificationCode(String email) {

        email = email.trim().toLowerCase();

        String code = createVerificationCode();

        // 새 인증번호를 받으면 기존 인증 상태 제거
        verifiedEmails.remove(email);

        // 인증번호 저장
        verificationCodes.put(email, code);

        // 만료시간 저장
        expirationTimes.put(
                email,
                System.currentTimeMillis() + EXPIRATION_TIME
        );

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(
                senderUsername + "@naver.com"
        );

        message.setTo(email);

        message.setSubject(
                "[SafeHouse] 이메일 인증번호"
        );

        message.setText(
                "SafeHouse 이메일 인증번호는 [" +
                code +
                "] 입니다.\n\n" +
                "인증번호는 5분 동안 유효합니다."
        );

        mailSender.send(message);
    }

    // 인증번호 확인
    public boolean verifyCode(
            String email,
            String code) {

        email = email.trim().toLowerCase();
        code = code.trim();

        String savedCode =
                verificationCodes.get(email);

        Long expirationTime =
                expirationTimes.get(email);

        if (savedCode == null ||
                expirationTime == null) {

            return false;
        }

        // 5분 초과
        if (System.currentTimeMillis()
                > expirationTime) {

            verificationCodes.remove(email);
            expirationTimes.remove(email);

            return false;
        }

        // 인증번호 불일치
        if (!savedCode.equals(code)) {
            return false;
        }

        // 인증 성공
        verificationCodes.remove(email);
        expirationTimes.remove(email);

        // 인증 완료 이메일로 저장
        verifiedEmails.add(email);

        return true;
    }

    // 이메일 인증 여부 확인
    public boolean isVerified(String email) {

        if (email == null) {
            return false;
        }

        email = email.trim().toLowerCase();

        return verifiedEmails.contains(email);
    }

    // 회원가입 성공 후 인증 상태 삭제
    public void clearVerification(String email) {

        if (email == null) {
            return;
        }

        email = email.trim().toLowerCase();

        verifiedEmails.remove(email);
    }

    // 네이버 로그인 알림 이메일 전송
    public void sendNaverLoginNotification(String email) {

        if (email == null || email.isBlank()) {
            return;
        }

        email = email.trim().toLowerCase();

        String loginTime =
                LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern(
                                "yyyy-MM-dd HH:mm:ss"
                        )
                );

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(
                senderUsername + "@naver.com"
        );

        message.setTo(email);

        message.setSubject(
                "[SafeHouse] 네이버 로그인 알림"
        );

        message.setText(
                "SafeHouse에 네이버 계정으로 로그인되었습니다.\n\n" +
                "로그인 시간: " + loginTime + "\n\n" +
                "본인이 로그인한 것이 아니라면 " +
                "네이버 계정의 보안 상태를 확인해주세요."
        );

        mailSender.send(message);
    }
}