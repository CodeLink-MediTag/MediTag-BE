package com.example.meditag.domain.chatbot.entity;

import com.example.meditag.domain.chatbot.dto.MessageDTO;
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
public class FAQ extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length=1000, nullable=false)
    private String question;

    @Column(length=2000, nullable=false)
    private String answer;

    // 추가된 액션 필드
    @Enumerated(EnumType.STRING)
    private MessageDTO.ActionDTO.ActionType actionType;

    private String actionTarget;
}
