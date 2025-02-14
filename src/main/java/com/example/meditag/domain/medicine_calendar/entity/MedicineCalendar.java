package com.example.meditag.domain.medicine_calendar.entity;

import com.example.meditag.domain.alarm.entity.Alarm;
import com.example.meditag.domain.calendar.entity.Calendar;
import com.example.meditag.domain.medicine.entity.Medicine;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MedicineCalendar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //약 연관관계 매핑(다대일)
    @ManyToOne
    @JoinColumn(name = "medicine_id")
    private Medicine medicine;

    //캘린더 연관관계 매핑(다대일)
    @ManyToOne
    @JoinColumn(name = "calendar_id")
    private Calendar calendar;

    //알림 연관관계 매핑(일다대)
    @OneToMany(mappedBy = "medicineCalendar", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Alarm> alarms = new ArrayList<>();
}
