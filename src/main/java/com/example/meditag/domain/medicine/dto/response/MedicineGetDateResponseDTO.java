package com.example.meditag.domain.medicine.dto.response;

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
public class MedicineGetDateResponseDTO {

    private String date;  // 날짜 (예: 2025-03-01)
    private List<MedicineDTO> medicines;  // 해당 날짜에 복용해야 하는 약 리스트

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class MedicineDTO {
        private String medicineName;  // 약 이름
        private String characteristic;  // 약 특징
        private String imageUrl;  // 약 이미지 URL
        private List<AlarmDTO> alarms;  // 알림 시간 리스트
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

