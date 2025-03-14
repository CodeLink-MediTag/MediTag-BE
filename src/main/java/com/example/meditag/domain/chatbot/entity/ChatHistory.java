package com.example.meditag.domain.chatbot.entity;

import com.example.meditag.domain.common.model.BaseTimeEntity;
import com.example.meditag.domain.member.entity.Member;
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
public class ChatHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String userMessage;

    @Column(columnDefinition = "TEXT")
    private String botResponse;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;
}
