package com.example.meditag.domain.medicine.entity;


import com.example.meditag.domain.alarm.entity.Alarm;
import com.example.meditag.domain.calendar.entity.Calendar;
import com.example.meditag.domain.common.model.BaseTimeEntity;
import com.example.meditag.domain.medicine.dto.request.MedicineCreateRequestDTO;
import com.example.meditag.domain.medicine.dto.request.MedicineUpdateRequestDto;
import com.example.meditag.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Medicine extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //약 이름
    private String name;

    //특징
    private String characteristic;

    //복용 시작 날짜
    private LocalDate startDate;

    //복용 기간
    private int duration;

    //복용 횟수(일반약 일때)
    private int frequency;

    //사진
    private String imageUrl;

    //처방약 여부
    private Boolean prescribed;

    //회원 연관관계 매핑 (다대일)
    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    //날짜 연관관계 매핑 (일대다)
    @OneToMany(mappedBy = "medicine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Calendar> calendars = new ArrayList<>();

    public void update(MedicineUpdateRequestDto dto, String imageUrl) {
        if (dto.getName() != null) this.name = dto.getName();
        if (dto.getCharacteristic() != null) this.characteristic = dto.getCharacteristic();
        if (dto.getStartDate() != null) this.startDate = dto.getStartDate();
        if (dto.getDuration() != null) this.duration = dto.getDuration();
        if (dto.getFrequency() != null) this.frequency = dto.getFrequency();
        if (dto.getPrescribed() != null) this.prescribed = dto.getPrescribed();

        if (imageUrl != null) {
            this.imageUrl = imageUrl;
        }
    }
}
