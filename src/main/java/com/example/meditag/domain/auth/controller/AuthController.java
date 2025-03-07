package com.example.meditag.domain.auth.controller;

import com.example.meditag.domain.auth.controller.api.AuthApi;
import com.example.meditag.domain.auth.dto.LoginDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController implements AuthApi {

    @PostMapping("/login")
    public ResponseEntity<String> login(LoginDTO loginDTO) {
        // 실제 인증은 LoginFilter에서 처리되므로 이 메서드는 Swagger 문서화를 위한 용도로만 사용됩니다.
        return null;
    }

    // 로그아웃
//    @PostMapping("/logout")
//    public ResponseEntity<String> logout() {
//        // 클라이언트가 JWT를 제거하면 됨 (서버에서 블랙리스트 저장 가능)
//        return ResponseEntity.ok("로그아웃 성공!");
//    }
}
