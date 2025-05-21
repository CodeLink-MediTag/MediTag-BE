package com.example.meditag.domain.medicine.controller.api;

import com.example.meditag.domain.auth.dto.CustomUserDetails;
import com.example.meditag.domain.medicine.dto.request.MedicineCreateRequestDTO;
import com.example.meditag.domain.medicine.dto.request.MedicineUpdateRequestDto;
import com.example.meditag.domain.medicine.dto.response.MedicineGetDateResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "약 관리", description = "약 관련 API")
public interface MedicineApi {

    @Operation(summary = "약 정보 등록", description = "새로운 약 정보를 등록합니다.\n" +
            "```json\n" +
            "{\n" +
            "  \"name\": \"처방약\",\n" +
            "  \"characteristic\": \"겁나 써\",\n" +
            "  \"startDate\": \"2025-03-28\",\n" +
            "  \"duration\": 7,\n" +
            "  \"prescribed\": true,\n" +
            "  \"dosageTimes\": [\n" +
            "    \"아침\",\n" +
            "    \"점심\",\n" +
            "    \"저녁\"\n" +
            "  ],\n" +
            "  \"alarmTimes\": [\n" +
            "    \"08:00:00\",\n" +
            "    \"12:00:00\",\n" +
            "    \"18:00:00\"\n" +
            "  ]\n" +
            "}\n" +
            "\n" +
            "{\n" +
            "  \"name\": \"일반약\",\n" +
            "  \"characteristic\": \"겁나 써\",\n" +
            "  \"startDate\": \"2025-03-28\",\n" +
            "  \"duration\": 7,\n" +
            "  \"frequency\": 3,\n" +
            "  \"prescribed\": false,\n" +
            "  \"alarmTimes\": [\n" +
            "    \"08:00:00\",\n" +
            "    \"12:00:00\",\n" +
            "    \"18:00:00\"\n" +
            "  ]\n" +
            "}\n" +
            "```")

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    ResponseEntity<String> createMedicine(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "약 정보", required = true)
            @RequestPart(value = "data") MedicineCreateRequestDTO requestDto,
            @Parameter(description = "약 이미지")
            @RequestPart(value = "file", required = false) MultipartFile file
    );

    @Operation(summary = "약 정보 수정", description = "기존의 약 정보를 수정합니다.\n" +
            "수정 시 `medicineId`를 경로로 전달하고, `data`에 수정할 정보와 선택적으로 이미지 파일을 함께 전달합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "404", description = "약 정보 없음"),
            @ApiResponse(responseCode = "400", description = "요청 형식 오류")
    })
    @PatchMapping(value = "/{medicineId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<String> updateMedicine(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "약 ID", required = true) @PathVariable Long medicineId,
            @Parameter(description = "수정할 약 정보", required = true)
            @RequestPart("data") MedicineUpdateRequestDto requestDto,
            @Parameter(description = "수정할 약 이미지")
            @RequestPart(value = "file", required = false) MultipartFile file
    );

    @Operation(summary = "약 정보 삭제", description = "지정한 ID의 약 정보를 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "약 정보 없음")
    })
    @DeleteMapping("/{medicineId}")
    ResponseEntity<String> deleteMedicine(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "삭제할 약 ID", required = true) @PathVariable Long medicineId
    );

    @Operation(summary = "날짜별 약 정보 조회", description = "특정 날짜의 약 정보를 조회합니다.\n" +
            "```json\n" +
            "2025-03-28\n" +
            "```")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 날짜 정보 없음")
    })
    ResponseEntity<MedicineGetDateResponseDTO> getMedicinesByDate(
            @Parameter(description = "사용자 정보") @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "조회할 날짜 (YYYY-MM-DD)", required = true) @RequestParam String date
    );

    @Operation(summary = "약 이미지 URL 조회", description = "약 이미지의 Presigned URL을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "URL 조회 성공")
    })
    String getPresignedUrl(@Parameter(description = "파일명", required = true) @RequestParam String filename);
}