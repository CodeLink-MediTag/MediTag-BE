package com.example.meditag.domain.calendar.entity;

import com.example.meditag.domain.medicine.entity.Medicine;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Calendar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //날짜
    private LocalDate date;

    //약 연관관계 매핑(다대일)
    @ManyToOne
    @JoinColumn(name = "medicine_id") // 외래키 이름 지정
    private Medicine medicine;
}
