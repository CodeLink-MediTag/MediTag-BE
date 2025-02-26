package com.example.meditag.domain.medicine.controller;

import com.example.meditag.domain.auth.dto.CustomUserDetails;
import com.example.meditag.domain.medicine.dto.request.MedicineCreateRequestDTO;

import com.example.meditag.domain.medicine.dto.response.MedicineCreateResponseDTO;
import com.example.meditag.domain.medicine.service.MedicineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/medicines")
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService medicineService;

    //약 알림 등록
    @PostMapping
    public ResponseEntity<String> saveMedicine(@AuthenticationPrincipal CustomUserDetails customUserDetails, @RequestBody MedicineCreateRequestDTO requestDto) {

        medicineService.saveMedicine(customUserDetails.getUsername(), requestDto);

        return ResponseEntity.ok("약이 성공적으로 저장되었습니다.");
    }


}
