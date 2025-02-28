package com.example.meditag.domain.calendar.service;

import com.example.meditag.domain.calendar.entity.Calendar;
import com.example.meditag.domain.calendar.repository.CalendarRepository;
import com.example.meditag.domain.medicine.entity.Medicine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final CalendarRepository calendarRepository;

    // 복약 날짜에 맞춰 캘린더 자동 생성
    @Transactional
    public void createCalendarForMedicine(Medicine medicine, LocalDate startDate, int duration) {
        List<Calendar> calendarList = new ArrayList<>();

        // 복용 시작 날짜부터 duration 만큼 날짜를 계산하여 Calendar 엔티티 생성
        for (int i = 0; i < duration; i++) {
            LocalDate medicineDate = startDate.plusDays(i);  // 시작일로부터 duration 만큼 날짜 생성
            Calendar calendar = Calendar.builder()
                    .date(medicineDate) // 날짜를 String 형식으로 저장
                    .medicine(medicine)
                    .build();
            calendarList.add(calendar);
        }

        // 생성한 캘린더 리스트를 DB에 저장
        calendarRepository.saveAll(calendarList);
    }
}
