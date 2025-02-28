package com.example.meditag.domain.medicine.controller;

import com.example.meditag.domain.auth.dto.CustomUserDetails;
import com.example.meditag.domain.medicine.dto.request.MedicineCreateRequestDTO;
import com.example.meditag.domain.medicine.dto.response.MedicineGetDateResponseDTO;
import com.example.meditag.domain.medicine.service.MedicineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/medicines")
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService medicineService;

    // 복약 알림 등록 API
    @PostMapping
    public ResponseEntity<String> createMedicine(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                 @RequestPart(value = "data") MedicineCreateRequestDTO requestDto,
                                                 @RequestPart(value = "file", required = false) MultipartFile file) {

        medicineService.createMedicine(customUserDetails.getUsername(), requestDto, file);

        return ResponseEntity.ok("약 정보와 알림이 성공적으로 저장되었습니다.");
    }

    // 특정 날짜 복약 정보 조회 API

    // 복용 여부 API

    // 사진 URL 반환
    @GetMapping("/presigned-url")
    public String getPresignedUrl(@RequestParam String filename) {
        return medicineService.getPresignedUrl(filename);
    }
}