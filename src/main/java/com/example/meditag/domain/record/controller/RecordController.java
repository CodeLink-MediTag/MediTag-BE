package com.example.meditag.domain.record.controller;

import com.example.meditag.domain.auth.dto.CustomUserDetails;
import com.example.meditag.domain.record.controller.api.RecordApi;
import com.example.meditag.domain.record.dto.RecordingCreateRequestDTO;
import com.example.meditag.domain.record.dto.RecordingResponseDTO;
import com.example.meditag.domain.record.service.RecordService;
import com.example.meditag.global.aws.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor

public class RecordController implements RecordApi {

    private final RecordService recordService;


    @PostMapping
    public ResponseEntity<String> createRecord(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                 @RequestPart(value = "data") RecordingCreateRequestDTO requestDTO,
                                                 @RequestPart(value = "file", required = false) MultipartFile file) {

        recordService.createRecord(customUserDetails.getUsername(), requestDTO, file);

        return ResponseEntity.ok("녹음파일이 저장되었습니다.");
    }

    @GetMapping
    public ResponseEntity<List<RecordingResponseDTO>> getAllRecordings(@AuthenticationPrincipal CustomUserDetails customUserDetails) {

        List<RecordingResponseDTO> getallrecording = recordService.getAllRecordings(customUserDetails.getUsername());
        return ResponseEntity.ok(getallrecording);
    }


}
