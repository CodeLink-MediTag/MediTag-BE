package com.example.meditag.global.sms.service;

import lombok.extern.slf4j.Slf4j;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.response.SingleMessageSentResponse;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Slf4j
@Service
public class SmsService {

    @Value("${coolsms.api.key}")
    private String apiKey;

    @Value("${coolsms.api.secret}")
    private String apiSecret;

    @Value("${coolsms.api.number}")
    private String fromPhoneNumber;

    private DefaultMessageService messageService;

    @PostConstruct
    private void init() {
        this.messageService = NurigoApp.INSTANCE.initialize(apiKey, apiSecret, "https://api.coolsms.co.kr");
    }

    /**
     * 단일 SMS 메시지 발송
     * @param to 수신자 전화번호
     * @param content 메시지 내용
     * @return 발송 결과
     */
    public SingleMessageSentResponse sendSms(String to, String content) {
        Message message = new Message();
        message.setFrom(fromPhoneNumber);
        message.setTo(to);
        message.setText(content);

        try {
            SingleMessageSentResponse response = this.messageService.sendOne(new SingleMessageSendingRequest(message));
            log.info("[SmsService] SMS 발송 완료 - 수신자: {}", to);
            return response;
        } catch (Exception e) {
            log.error("[SmsService] SMS 발송 실패: {}", e.getMessage(), e);
            throw new RuntimeException("SMS 발송 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 약 복용 알림 SMS 발송
     * @param to 수신자 전화번호
     * @param memberName 회원 이름
     * @param medicineName 약 이름
     * @return 발송 결과
     */
    public SingleMessageSentResponse sendMedicationNotification(String to, String memberName, String medicineName) {
        String content = String.format("메디테그알림 :📍 %s 님이 %s을(를) 복용했어요! 오늘도 건강관리 잘하고 있어요 😄", 
                memberName, medicineName);
        return sendSms(to, content);
    }
    public SingleMessageSentResponse sendTestSms(String to) {
        String content = "test-메디테그알림 :📍 %s 님이 %s을(를) 복용했어요! 오늘도 건강관리 잘하고 있어요 😄";
        return sendSms(to, content);
    }
}
