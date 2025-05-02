package com.example.meditag.global.sms.controller;

import com.example.meditag.global.sms.dto.SmsRequestDTO;
import com.example.meditag.global.sms.service.SmsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.nurigo.sdk.message.response.SingleMessageSentResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
@Tag(name = "SMS API", description = "SMS 발송 관련 API")
public class SmsController {

    private final SmsService smsService;

    @Operation(summary = "테스트 SMS 발송", description = "지정된 전화번호로 테스트 SMS를 발송합니다.")
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> sendTestSms(@RequestBody SmsRequestDTO request) {
        log.info("[SmsController] 테스트 SMS 발송 요청 - 전화번호: {}", request.getPhoneNumber());

        try {
            SingleMessageSentResponse response = smsService.sendTestSms(request.getPhoneNumber());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "test:\"메디테그알림\"📍 메디 님이 감기 처방약을 복용했어요! 오늘도 건강관리 잘하고 있어요 😄.");
            result.put("messageId", response.getMessageId());
            result.put("to", response.getTo());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[SmsController] 테스트 SMS 발송 실패: {}", e.getMessage(), e);

            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "SMS 발송에 실패했습니다: " + e.getMessage());

            return ResponseEntity.badRequest().body(result);
        }
    }

//    @Operation(summary = "약 복용 알림 SMS 발송", description = "지정된 전화번호로 약 복용 알림 SMS를 발송합니다.")
//    @PostMapping("/medicine-reminder")
//    public ResponseEntity<Map<String, Object>> sendMedicineReminder(@RequestBody MedicineReminderDTO request) {
//        log.info("[SmsController] 약 복용 알림 SMS 발송 요청 - 전화번호: {}, 약 이름: {}",
//                request.getPhoneNumber(), request.getMedicineName());
//
//        try {
//            SingleMessageSentResponse response = smsService.sendMedicineReminder(
//                    request.getPhoneNumber(), request.getMedicineName());
//
//            Map<String, Object> result = new HashMap<>();
//            result.put("success", true);
//            result.put("message", "약 복용 알림이 성공적으로 발송되었습니다.");
//            result.put("messageId", response.getMessageId());
//            result.put("to", response.getTo());
//
//            return ResponseEntity.ok(result);
//        } catch (Exception e) {
//            log.error("[SmsController] 약 복용 알림 SMS 발송 실패: {}", e.getMessage(), e);
//
//            Map<String, Object> result = new HashMap<>();
//            result.put("success", false);
//            result.put("message", "약 복용 알림 발송에 실패했습니다: " + e.getMessage());
//
//            return ResponseEntity.badRequest().body(result);
//        }
//    }
}