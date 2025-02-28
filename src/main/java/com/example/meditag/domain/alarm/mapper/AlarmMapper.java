package com.example.meditag.domain.alarm.mapper;

import com.example.meditag.domain.alarm.dto.response.AlarmResponseDTO;
import com.example.meditag.domain.alarm.entity.Alarm;
import com.example.meditag.domain.medicine.entity.Medicine;

import java.time.LocalDateTime;
import java.util.List;

public class AlarmMapper {

    // 처방약 알림
    public static Alarm toAlarmsForPrescribedMedicine (Medicine medicine, String dosageTime, LocalDateTime alarmTime) {
        return Alarm.builder()
                .medicine(medicine)
                .dosageTime(dosageTime)
                .alarmTime(alarmTime)
                .taking(false)
                .build();
    }

    // 일반약 알림
    public static Alarm toAlarmsForNormalMedicine (Medicine medicine, LocalDateTime alarmTime) {
        return Alarm.builder()
                .medicine(medicine)
                .alarmTime(alarmTime)
                .taking(false)
                .build();
    }

    public static AlarmResponseDTO toAlarmResponseDTO(Alarm alarm) {
        return AlarmResponseDTO.builder()
                .id(alarm.getId())
                .dosageTime(alarm.getDosageTime())
                .alarmTime(alarm.getAlarmTime())
                .taking(alarm.isTaking())
                .build();
    }
}
