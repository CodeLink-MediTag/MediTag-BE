package com.example.meditag.domain.alarm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AlarmResponseDTO {

    private Long id;

    //시간대 (아침, 점심, 저녁)
    private String dosageTime;

    //알림 시간
    private LocalDateTime alarmTime;

    //복용 여부
    private boolean taking;
}
