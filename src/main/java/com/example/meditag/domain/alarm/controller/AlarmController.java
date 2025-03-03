package com.example.meditag.domain.alarm.controller;

import com.example.meditag.domain.alarm.service.AlarmService;
import com.example.meditag.domain.auth.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;

    // 복용 여부 API
    @PatchMapping("/{medicineId}/alarm/{alarmId}/taking")
    public ResponseEntity<String> toggleTaking(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                               @PathVariable("medicineId") Long medicineId,
                                               @PathVariable("alarmId") Long alarmId) {

        alarmService.toggleTaking(customUserDetails.getUsername(), medicineId, alarmId);

        return ResponseEntity.ok("복용 여부가 변경되었습니다.");
    }
}
