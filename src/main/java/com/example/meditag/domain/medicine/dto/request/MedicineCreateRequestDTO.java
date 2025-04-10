package com.example.meditag.domain.medicine.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MedicineCreateRequestDTO {

    // 약 이름
    private String name;

    // 특징
    private String characteristic;

    // 복용 시작 날짜
    private LocalDate startDate;

    // 복용 기간
    private int duration;

    // 복용 횟수(일반약 일때)
    private int frequency;

    // 처방약 여부
    private boolean prescribed;

    // 처방약일 경우(isPrescribed=true): 아침, 점심, 저녁 등 복용 시간대 선택 (예: ["아침", "저녁"])
    private List<String> dosageTimes;

    // 일반약일 경우(isPrescribed=false): 알람 시간 목록
    private List<String> alarmTimes; // "08:00", "12:00" 같은 문자열
}