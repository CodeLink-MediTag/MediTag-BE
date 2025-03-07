package com.example.meditag.domain.alarm.mapper;

import com.example.meditag.domain.alarm.dto.response.AlarmResponseDTO;
import com.example.meditag.domain.alarm.entity.Alarm;

public class AlarmMapper {

    public static AlarmResponseDTO toAlarmResponseDTO(Alarm alarm) {
        return AlarmResponseDTO.builder()
                .id(alarm.getId())
                .dosageTime(alarm.getDosageTime())
                .alarmTime(alarm.getAlarmTime())
                .taking(alarm.isTaking())
                .build();
    }
}
