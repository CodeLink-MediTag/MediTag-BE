package com.example.meditag.domain.chatbot.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSessionDTO {
    private Long id;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Long memberId; // 연관된 사용자 ID
}

