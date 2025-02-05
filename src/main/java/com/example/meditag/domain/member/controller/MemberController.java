package com.example.meditag.domain.member.controller;

import com.example.meditag.domain.member.dto.request.RegisterDTO;
import com.example.meditag.domain.member.service.RegisterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/member")
public class MemberController {

    private final RegisterService registerService;

    public MemberController(RegisterService registerService) {
        this.registerService = registerService;
    }

    // 회원가입
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody RegisterDTO registerDTO) {

        return ResponseEntity.ok(Map.of("message", "회원가입 성공"));
    }
}
