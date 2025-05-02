package com.example.meditag.domain.member.controller;

import com.example.meditag.domain.auth.dto.CustomUserDetails;
import com.example.meditag.domain.member.dto.request.GuardianRequestDTO;
import com.example.meditag.domain.member.dto.response.GuardianResponseDTO;
import com.example.meditag.domain.member.service.GuardianService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/guardians")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Guardian API", description = "보호자 관리 API")
public class GuardianController {

    private final GuardianService guardianService;

    @Operation(summary = "보호자 등록", description = "새로운 보호자를 등록합니다.")
    @PostMapping
    public ResponseEntity<GuardianResponseDTO> registerGuardian(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody GuardianRequestDTO requestDTO) {
        
        log.info("[GuardianController] 보호자 등록 요청 - 회원: {}, 전화번호: {}", 
                userDetails.getUsername(), requestDTO.getPhoneNumber());
        
        GuardianResponseDTO responseDTO = guardianService.registerGuardian(
                userDetails.getUsername(), requestDTO);
        
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(summary = "보호자 목록 조회", description = "회원의 모든 보호자 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<GuardianResponseDTO>> getGuardians(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        log.info("[GuardianController] 보호자 목록 조회 요청 - 회원: {}", 
                userDetails.getUsername());
        
        List<GuardianResponseDTO> guardians = guardianService.getGuardians(
                userDetails.getUsername());
        
        return ResponseEntity.ok(guardians);
    }

    @Operation(summary = "보호자 삭제", description = "특정 보호자를 삭제합니다.")
    @DeleteMapping("/{guardianId}")
    public ResponseEntity<Map<String, String>> deleteGuardian(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long guardianId) {
        
        log.info("[GuardianController] 보호자 삭제 요청 - 회원: {}, 보호자 ID: {}", 
                userDetails.getUsername(), guardianId);
        
        guardianService.deleteGuardian(userDetails.getUsername(), guardianId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "보호자가 성공적으로 삭제되었습니다.");
        
        return ResponseEntity.ok(response);
    }
}
