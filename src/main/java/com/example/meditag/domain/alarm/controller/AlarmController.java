package com.example.meditag.domain.alarm.controller;

import com.example.meditag.domain.alarm.controller.api.AlarmApi;
import com.example.meditag.domain.alarm.service.AlarmService;
import com.example.meditag.domain.auth.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@RestController
@RequestMapping("/api/medicines")
@RequiredArgsConstructor
public class AlarmController implements AlarmApi {

    private final AlarmService alarmService;

    // 복용 여부 API
    @PatchMapping("/{medicineId}/alarms/taking")
    public ResponseEntity<String> toggleTakingByType(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable("medicineId") Long medicineId,
            @RequestParam(value = "dosageTime", required = false) String dosageTime,
            @RequestParam(value = "alarmTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime alarmTime,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        alarmService.toggleTakingByType(
                customUserDetails.getUsername(), medicineId, date, dosageTime, alarmTime
        );

        return ResponseEntity.ok("복용 여부가 변경되었습니다.");
    }
}
