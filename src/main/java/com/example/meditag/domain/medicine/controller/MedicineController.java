package com.example.meditag.domain.medicine.controller;

import com.example.meditag.domain.auth.dto.CustomUserDetails;
import com.example.meditag.domain.medicine.dto.request.MedicineCreateRequestDTO;

import com.example.meditag.domain.medicine.dto.response.MedicineCreateResponseDTO;
import com.example.meditag.domain.medicine.service.MedicineService;
import com.example.meditag.global.aws.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.util.UUID;

@RestController
@RequestMapping("/api/medicines")
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService medicineService;
    private final S3Service s3Service;

    @PostMapping("/reminders")
    public ResponseEntity<String> saveMedicine(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestPart("data") MedicineCreateRequestDTO requestDto,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        String imageUrl = null;
        
        // 파일이 있는 경우에만 S3 업로드 처리
        if (file != null && !file.isEmpty()) {
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            String presignedUrl = s3Service.createPresignedUrl("test/" + fileName);
            
            try {
                // S3에 직접 업로드
                URL url = new URL(presignedUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setRequestMethod("PUT");
                connection.setRequestProperty("Content-Type", file.getContentType());
                
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(file.getBytes());
                }
                
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    imageUrl = presignedUrl.split("\\?")[0];  // URL에서 query parameter 제거
                }
            } catch (Exception e) {
                throw new RuntimeException("파일 업로드 실패", e);
            }
        }

        // Builder 패턴을 사용하여 새로운 DTO 생성
        MedicineCreateRequestDTO finalRequestDto = MedicineCreateRequestDTO.builder()
                .name(requestDto.getName())
                .characteristic(requestDto.getCharacteristic())
                .startDate(requestDto.getStartDate())
                .duration(requestDto.getDuration())
                .frequency(requestDto.getFrequency())
                .isPrescribed(false)
                .imageUrl(imageUrl)  // 업로드된 이미지 URL 설정
                .build();

        medicineService.saveMedicine(customUserDetails.getUsername(), finalRequestDto);
        return ResponseEntity.ok("약이 성공적으로 저장되었습니다.");
    }

    @GetMapping("/presigned-url")
    @ResponseBody
    String getUrl(@RequestParam String filename){
        var result = s3Service.createPresignedUrl("test/"+filename);
        return result;
    }
}
