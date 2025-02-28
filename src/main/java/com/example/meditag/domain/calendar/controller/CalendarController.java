package com.example.meditag.domain.calendar.controller;

import com.example.meditag.domain.auth.dto.CustomUserDetails;
import com.example.meditag.domain.calendar.dto.response.CalendarGetDateResponseDTO;
import com.example.meditag.domain.calendar.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    // 특정 날짜 복약 정보 조회 API
    @GetMapping
    public ResponseEntity<CalendarGetDateResponseDTO> getCalendarByDate(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                                        @RequestParam String date) {

        CalendarGetDateResponseDTO responseDTO = calendarService.getCalendarByDate(customUserDetails.getUsername(), date);

        return ResponseEntity.ok(responseDTO);
    }
}
