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

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginService loginService;
    private final CustomUserDetailsService customUserDetailsService;

    public AuthController(LoginService loginService, CustomUserDetailsService customUserDetailsService) {
        this.loginService = loginService;
        this.customUserDetailsService = customUserDetailsService;
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<TokenDTO> login(@RequestBody LoginDTO loginDTO) {

        return ResponseEntity.ok(new TokenDTO());
    }
}
