package com.example.meditag.domain.auth.controller;

import com.example.meditag.domain.auth.dto.request.LoginDTO;
import com.example.meditag.domain.auth.dto.response.TokenDTO;
import com.example.meditag.domain.auth.service.CustomUserDetailsService;
import com.example.meditag.domain.auth.service.LoginService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginService loginService;

    public AuthController(LoginService loginService) {
        this.loginService = loginService;
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<Map<String, TokenDTO>> login(@RequestBody LoginDTO loginDTO) {
        TokenDTO tokenDTO = loginService.login(loginDTO);
        return ResponseEntity.ok(Map.of("로그인 성공", tokenDTO));
    }
}
