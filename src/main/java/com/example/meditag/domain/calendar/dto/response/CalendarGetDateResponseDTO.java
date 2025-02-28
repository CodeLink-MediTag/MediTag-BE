package com.example.meditag.domain.calendar.dto.response;

import com.example.meditag.domain.medicine.dto.response.MedicineGetDateResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CalendarGetDateResponseDTO {

    private String date;  // 날짜 (예: 2025-03-01)
    private List<CalendarGetDateResponseDTO.calendarDTO> medicines;  // 해당 날짜에 복용해야 하는 약 리스트

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class calendarDTO {
        private String medicineName;  // 약 이름
        private boolean prescribed; // 처방약 여부
        private List<CalendarGetDateResponseDTO.AlarmDTO> alarms;  // 알림 시간 리스트
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class AlarmDTO {
        private LocalDateTime alarmTime;  // 알람 시간
        private boolean isTaking;  // 복용 여부
    }
}
