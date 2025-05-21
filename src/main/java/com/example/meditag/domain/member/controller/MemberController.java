package com.example.meditag.domain.member.controller;

import com.example.meditag.domain.auth.dto.CustomUserDetails;
import com.example.meditag.domain.member.controller.api.MemberApi;
import com.example.meditag.domain.member.dto.request.RegisterDTO;
import com.example.meditag.domain.member.dto.request.UpdateDTO;
import com.example.meditag.domain.member.service.RegisterService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.Map;

@RestController
@RequestMapping("/api/member")
public class MemberController implements MemberApi {

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

    // 회원 수정
    @PatchMapping("/me")
    public ResponseEntity<String> updateMember(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody UpdateDTO updateDTO) {
        registerService.updateMember(userDetails.getUsername(), updateDTO);
        return ResponseEntity.ok("회원 정보가 수정되었습니다.");
    }

    // 회원 삭제
    @DeleteMapping("/me")
    public ResponseEntity<String> deleteMember(@AuthenticationPrincipal CustomUserDetails userDetails) {
        registerService.deleteMember(userDetails.getUsername());
        return ResponseEntity.ok("회원이 삭제되었습니다.");
    }

    // 회원 조회
    @GetMapping("/me")
    public ResponseEntity<RegisterDTO> getMember(@AuthenticationPrincipal CustomUserDetails userDetails) {
        RegisterDTO member = registerService.getMemberByUsername(userDetails.getUsername());
        return ResponseEntity.ok(member);
    }
}
