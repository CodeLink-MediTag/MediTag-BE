package com.example.meditag.domain.alarm.service;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class FCMService {

    /**
     *  token FCM 디바이스 토큰
     *  title 알림 제목
     *  body 알림 내용
     *  return 전송 결과 (메시지 ID)
     */
    public String sendPushNotification(String token, String title, String body){
        if(token ==null || token.isEmpty()){
            log.warn("token 비어있거나 null 입니다");
            return null;
        }
        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("type","medication_reminder")
                    .setAndroidConfig(AndroidConfig.builder() // ✅ Android 설정 추가
                            .setPriority(AndroidConfig.Priority.HIGH) // ✅ 중요도 설정 (Heads-up Notification을 위해 필수) 근데 안됨
                            .build())
                    .build();

            String response = FirebaseMessaging.getInstance().sendAsync(message).get();
            log.info("(FCMService)알림 전송성공",response);
            return response;
        } catch (ExecutionException e) {
            log.error("[FCMService] 알림 전송 중 인터럽트 발생", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}