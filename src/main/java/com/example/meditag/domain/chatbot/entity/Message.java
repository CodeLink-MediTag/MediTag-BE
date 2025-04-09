package com.example.meditag.domain.chatbot.entity;

import com.example.meditag.domain.common.model.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Message extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;

    @Enumerated(EnumType.STRING)
    private Sender sender; // USER or BOT

    @Column(nullable = false, length = 1000)
    private String content;

    public enum Sender {
        USER, BOT
    }

    // ChatSession과 Many-to-One 관계 (여러 메시지가 하나의 세션에 속함)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_session_id")
    private ChatSession chatSession;
}

