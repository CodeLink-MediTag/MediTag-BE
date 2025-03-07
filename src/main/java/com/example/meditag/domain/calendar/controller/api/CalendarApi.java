package com.example.meditag.domain.calendar.controller.api;

import com.example.meditag.domain.auth.dto.CustomUserDetails;
import com.example.meditag.domain.calendar.dto.response.CalendarGetDateResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "캘린더 관리", description = "캘린더 관련 API")
public interface CalendarApi {

    @Operation(summary = "날짜별 복약 정보 조회", description = "특정 날짜의 복약 정보를 조회합니다.\n" +
            "```json\n" +
            "2025-03-28\n" +
            "```")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 날짜 정보 없음")
    })
    ResponseEntity<CalendarGetDateResponseDTO> getCalendarByDate(
            @Parameter(description = "사용자 정보") @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "조회할 날짜 (YYYY-MM-DD)", required = true) @RequestParam String date
    );
}