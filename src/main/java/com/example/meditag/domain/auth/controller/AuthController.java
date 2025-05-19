package com.example.meditag.domain.auth.controller;

import com.example.meditag.domain.auth.controller.api.AuthApi;
import com.example.meditag.domain.auth.dto.LoginDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    // 로그인 (실제 로그인 로직은 LoginFilter에서 처리)
    @PostMapping("/login")
    public ResponseEntity<String> login(LoginDTO loginDTO) {
        log.info("[AuthController] 로그인 요청: {}", loginDTO.getUsername());
        return ResponseEntity.ok("로그인 요청이 처리되었습니다.");
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        log.info("[AuthController/logout] 로그아웃 요청 처리");
        // 실제 로그아웃 처리는 JWTFilter에서 수행
        return ResponseEntity.ok("로그아웃 되었습니다.");
    }

}