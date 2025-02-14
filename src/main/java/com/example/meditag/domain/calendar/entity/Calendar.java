package com.example.meditag.domain.calendar.entity;

import com.example.meditag.domain.medicine_calendar.entity.MedicineCalendar;
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
public class Calendar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //날짜
    private String date;

    //약_캘린더 연관관계 매핑 (일대다)
    @OneToMany(mappedBy = "calendar", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MedicineCalendar> medicineCalendars = new ArrayList<>();
}
