package com.example.meditag.domain.calendar.service;

import com.example.meditag.domain.alarm.entity.Alarm;
import com.example.meditag.domain.alarm.repository.AlarmRepository;
import com.example.meditag.domain.calendar.dto.response.CalendarGetDateResponseDTO;
import com.example.meditag.domain.calendar.entity.Calendar;
import com.example.meditag.domain.calendar.repository.CalendarRepository;
import com.example.meditag.domain.medicine.entity.Medicine;
import com.example.meditag.domain.member.entity.Member;
import com.example.meditag.domain.member.repository.MemberRepository;
import com.example.meditag.global.error.exception.CustomException;
import com.example.meditag.global.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final CalendarRepository calendarRepository;
    private final MemberRepository memberRepository;
    private final AlarmRepository alarmRepository;

    //캘린더에서 특정 날짜에 복약 정보 조회
    @Transactional
    public CalendarGetDateResponseDTO getCalendarByDate (String username, String date) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        List<Calendar> calendars = calendarRepository.findByMemberAndDate(member, LocalDate.parse(date));

        if (calendars.isEmpty()) {
            throw new CustomException(ErrorCode.MEDICINE_NOT_FOUND_FOR_DATE);
        }

        // 해당 날짜에 복용해야 하는 약 리스트
        List<CalendarGetDateResponseDTO.calendarDTO> calendarDTOList = new ArrayList<>();
        for (Calendar calendar : calendars) {
            Medicine medicine = calendar.getMedicine();

            // 알림 시간 조회
            List<Alarm> alarms = alarmRepository.findByMedicineAndDate(medicine, LocalDate.parse(date));

            List<CalendarGetDateResponseDTO.AlarmDTO> alarmDTOs = alarms.stream()
                    .map(alarm -> CalendarGetDateResponseDTO.AlarmDTO.builder()
                            .alarmTime(alarm.getAlarmTime())
                            .isTaking(alarm.isTaking())
                            .build())
                    .collect(Collectors.toList());

            CalendarGetDateResponseDTO.calendarDTO medicineDTO = CalendarGetDateResponseDTO.calendarDTO.builder()
                    .medicineName(medicine.getName())
                    .prescribed(medicine.isPrescribed())
                    .alarms(alarmDTOs)
                    .build();

            calendarDTOList.add(medicineDTO);
        }

        // 최종 DTO 반환
        return CalendarGetDateResponseDTO.builder()
                .date(date)  // 요청된 날짜
                .medicines(calendarDTOList)  // 해당 날짜에 복용해야 할 약 리스트
                .build();
    }
}
