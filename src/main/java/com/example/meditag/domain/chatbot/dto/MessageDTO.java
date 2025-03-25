package com.example.meditag.domain.chatbot.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageDTO {
    private String sender;      // 발신자 (USER 또는 BOT)
    private String content;     // 메시지 내용
}
