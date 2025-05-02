package com.example.meditag.domain.alarm.controller.api;

import com.example.meditag.domain.auth.dto.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;


@Tag(name = "알림 관리", description = "알림 관련 API")
public interface AlarmApi {

    @Operation(summary = "복용 여부 변경", description = "약의 복용 여부를 시간대나 알람 기준으로 변경합니다.\n" +
            "```json\n" +
            "예시\n" +
            "  \"dosageTime\": \"아침\" or \"점심\" or \"저녁\"\n" +
            "  \"alarmTime\": \"2025-04-25T08:00:00\"\n" +
            "  \"date\": \"2025-04-25\"\n" +
            "```")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "404", description = "해당 알림 없음")
    })
    ResponseEntity<String> toggleTakingByType(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long medicineId,
            @RequestParam(value = "dosageTime", required = false) String dosageTime,
            @RequestParam(value = "alarmTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime alarmTime,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    );
}