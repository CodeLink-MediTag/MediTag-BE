package com.example.meditag.domain.record.entity;

import com.example.meditag.domain.common.model.BaseTimeEntity;
import com.example.meditag.domain.member.entity.Member;
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
public class Recording extends BaseTimeEntity { //기본 속성과 이름이 겹처서 자꾸 에러남. Record=> Recording

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //제목
    private String title;

    //녹음 시간
    private LocalDateTime recordingTime;

    //녹음 파일경로
    private String recordingFile;

    //회원 연관관계 매핑(다대일)
    @ManyToOne
    @JoinColumn(name="member_id")
    private Member member;
}
