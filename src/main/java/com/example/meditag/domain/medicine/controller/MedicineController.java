package com.example.meditag.domain.medicine.controller;

import com.example.meditag.domain.auth.dto.CustomUserDetails;
import com.example.meditag.domain.medicine.controller.api.MedicineApi;
import com.example.meditag.domain.medicine.dto.request.MedicineCreateRequestDTO;
import com.example.meditag.domain.medicine.dto.request.MedicineUpdateRequestDto;
import com.example.meditag.domain.medicine.dto.response.MedicineGetDateResponseDTO;
import com.example.meditag.domain.medicine.service.MedicineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/medicines")
@RequiredArgsConstructor
public class MedicineController implements MedicineApi {

    private final MedicineService medicineService;

    // 복약 알림 등록 API
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<String> createMedicine(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                 @RequestPart(value = "data") MedicineCreateRequestDTO requestDto,
                                                 @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        medicineService.createMedicine(customUserDetails.getUsername(), requestDto, file);
        return ResponseEntity.ok("약 정보와 알림이 성공적으로 저장되었습니다.");
    }

    // 복약 알림 수정 API
    @PatchMapping(value = "/{medicineId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateMedicine(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                 @PathVariable Long medicineId,
                                                 @RequestPart("data") MedicineUpdateRequestDto updateDto,
                                                 @RequestPart(value = "file", required = false) MultipartFile file) {
        medicineService.updateMedicine(customUserDetails.getUsername(), medicineId, updateDto, file);
        return ResponseEntity.ok("약 정보와 알림이 성공적으로 수정되었습니다.");
    }

    // 복약 알림 삭제 API
    @DeleteMapping("/{medicineId}")
    public ResponseEntity<String> deleteMedicine(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                 @PathVariable Long medicineId
    ) {
        medicineService.deleteMedicine(customUserDetails.getUsername(), medicineId);
        return ResponseEntity.ok("약 정보와 알림이 성공적으로 삭제되었습니다.");
    }

    // 특정 날짜 복약 정보 조회 API
    @GetMapping
    public ResponseEntity<MedicineGetDateResponseDTO> getMedicinesByDate(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                                         @RequestParam String date) {

        MedicineGetDateResponseDTO responseDTO = medicineService.getMedicinesByDate(customUserDetails.getUsername(), date);

        return ResponseEntity.ok(responseDTO);
    }

    // 사진 URL 반환
    @GetMapping("/presigned-url")
    public String getPresignedUrl(@RequestParam String filename) {
        return medicineService.getPresignedUrl(filename);
    }
}