package com.example.meditag.domain.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        // 클라이언트가 JWT를 제거하면 됨 (서버에서 블랙리스트 저장 가능)
        return ResponseEntity.ok("로그아웃 성공!");
    }
}
