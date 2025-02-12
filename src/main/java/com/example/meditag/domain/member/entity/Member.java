package com.example.meditag.domain.member.entity;

import com.example.meditag.domain.common.model.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username; // email

    private String name;

    private String phone;

    private String password;

    private String role;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt; // 최종 로그인 시간

    // 최종 로그인 시간을 갱신하는 메서드
    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }
}
