package com.example.meditag.domain.alarm.controller.api;

import com.example.meditag.domain.auth.dto.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "알림 관리", description = "알림 관련 API")
public interface AlarmApi {

    @Operation(summary = "알림 상태 변경", description = "약 복용 여부를 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "알림 상태 변경 성공"),
            @ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음")
    })
    ResponseEntity<String> toggleTaking(
            @Parameter(description = "사용자 정보") @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "약 ID", required = true) @PathVariable Long medicineId,
            @Parameter(description = "알림 ID", required = true) @PathVariable Long alarmId
    );
}