package com.example.meditag.domain.alarm.entity;

import com.example.meditag.domain.common.model.BaseTimeEntity;
import com.example.meditag.domain.medicine.entity.Medicine;
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
public class Alarm extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //시간대 (아침, 점심, 저녁)
    private String dosageTime;

    //알림 시간
    private LocalDateTime alarmTime;

    //복용 여부
    private boolean isTaking;

    //약 연관관계 매핑(다대일)
    @ManyToOne
    @JoinColumn(name = "medicine_id") // 외래키 이름 지정
    private Medicine medicine;
}
