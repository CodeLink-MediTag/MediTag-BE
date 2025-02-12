package com.example.meditag.domain.member.controller;

import com.example.meditag.domain.member.dto.request.RegisterDTO;
import com.example.meditag.domain.member.service.RegisterService;
import jakarta.validation.Valid;
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
    public ResponseEntity<Map<String, RegisterDTO>> register(@Valid @RequestBody RegisterDTO registerDTO) {
        RegisterDTO responseDto = registerService.registerProcess(registerDTO);
        return ResponseEntity.ok(Map.of("회원가입 성공", responseDto));
    }

    // 프로필
}
