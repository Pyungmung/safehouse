package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.User;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByUsername(String username);

    User findByNaverId(String naverId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}