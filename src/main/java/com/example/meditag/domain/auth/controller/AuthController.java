package com.example.meditag.domain.auth.controller;

import com.example.meditag.domain.auth.controller.api.AuthApi;
import com.example.meditag.domain.auth.dto.LoginDTO;
import com.example.meditag.global.jwt.JWTUtil;
import com.example.meditag.domain.jwt.dto.TokenDTO;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<String> login(LoginDTO loginDTO) {
        // 실제 인증은 LoginFilter에서 처리되므로 이 메서드는 Swagger 문서화를 위한 용도로만 사용됩니다.
        return null;
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        log.info("[AuthController/logout] 로그아웃 요청 처리");
        // 실제 로그아웃 처리는 CustomLogoutFilter에서 수행됩니다.
        return ResponseEntity.ok("로그아웃 요청이 처리되었습니다.");
    }

}