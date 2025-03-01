package com.example.meditag.domain.record.controller;

import com.example.meditag.domain.auth.dto.CustomUserDetails;
import com.example.meditag.domain.medicine.dto.request.MedicineCreateRequestDTO;
import com.example.meditag.domain.record.dto.RecordingCreateRequestDTO;
import com.example.meditag.domain.record.service.RecordService;
import com.example.meditag.global.aws.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/record")
@RequiredArgsConstructor

public class RecordController {
    private final RecordService recordService;
    private final S3Service s3Service;


    @PostMapping
    public ResponseEntity<String> createRecord(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                 @RequestPart(value = "data") RecordingCreateRequestDTO requestDTO,
                                                 @RequestPart(value = "file", required = false) MultipartFile file) {

        recordService.createRecord(customUserDetails.getUsername(), requestDTO, file);

        return ResponseEntity.ok("녹음파일이 저장되었습니다.");
    }


}
