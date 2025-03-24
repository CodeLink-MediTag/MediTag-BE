package com.example.meditag.domain.member.entity;

import com.example.meditag.domain.chatbot.entity.ChatSession;
import com.example.meditag.domain.common.model.BaseTimeEntity;
import com.example.meditag.domain.medicine.entity.Medicine;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.example.meditag.domain.record.entity.Recording;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 이메일 (유니크 값으로 설정 가능)
    private String username;

    // 이름, 전화번호 등 사용자 정보 필드들
    private String name;
    private String phone;

    // 비밀번호 (암호화 필요)
    private String password;

    // 권한 (예: ROLE_USER, ROLE_ADMIN 등)
    private String role;

    // 최종 로그인 시간 기록 필드
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // ChatSession과 One-to-Many 관계 (하나의 사용자가 여러 세션을 가질 수 있음)
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatSession> chatSessions = new ArrayList<>();

    // 녹음 파일과 One-to-Many 관계 (기존 설계 유지)
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Recording> recordings = new ArrayList<>();

    // 약물 정보와 One-to-Many 관계 (기존 설계 유지)
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Medicine> medicines = new ArrayList<>();

    // 최종 로그인 시간을 갱신하는 메서드
    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }
}

