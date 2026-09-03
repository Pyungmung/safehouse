package com.example.demo.service;

import java.util.UUID;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.User;
import com.example.demo.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    // 일반 회원가입
    public User signup(User user) {

        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("이미 사용 중인 아이디입니다.");
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("이미 사용 중인 이메일입니다.");
        }

        String encodedPassword =
                passwordEncoder.encode(user.getPassword());

        user.setPassword(encodedPassword);

        return userRepository.save(user);
    }

    // 일반 로그인
    public User login(String username, String password) {

        User user = userRepository.findByUsername(username);

        if (user == null) {
            throw new RuntimeException("존재하지 않는 사용자입니다.");
        }

        if (user.getPassword() == null ||
                !passwordEncoder.matches(
                        password,
                        user.getPassword())) {

            throw new RuntimeException(
                    "비밀번호가 일치하지 않습니다."
            );
        }

        return user;
    }

    // 네이버 로그인
    public User naverLogin(
            String naverId,
            String email) {

        // 이미 네이버로 가입한 회원인지 확인
        User existingUser =
                userRepository.findByNaverId(naverId);

        if (existingUser != null) {
            return existingUser;
        }

        // 같은 이메일로 일반 회원가입이 되어 있는지 확인
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException(
                    "이미 일반 회원가입으로 사용 중인 이메일입니다."
            );
        }

        // 처음 네이버 로그인한 회원 자동 생성
        User newUser = new User();

        newUser.setNaverId(naverId);

        newUser.setEmail(
                email.trim().toLowerCase()
        );

        newUser.setUsername(
                "naver_" + UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
        );

        // 네이버 회원은 일반 비밀번호 로그인을 사용할 수 없도록
        // 임의의 비밀번호를 생성해서 암호화하여 저장
        String randomPassword =
                UUID.randomUUID().toString();

        newUser.setPassword(
                passwordEncoder.encode(randomPassword)
        );

        return userRepository.save(newUser);
    }
}