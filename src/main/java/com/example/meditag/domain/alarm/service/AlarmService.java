package com.example.meditag.domain.alarm.service;

import com.example.meditag.domain.alarm.dto.response.AlarmResponseDTO;
import com.example.meditag.domain.alarm.entity.Alarm;
import com.example.meditag.domain.alarm.mapper.AlarmMapper;
import com.example.meditag.domain.alarm.repository.AlarmRepository;
import com.example.meditag.domain.medicine.entity.Medicine;
import com.example.meditag.domain.medicine.repository.MedicineRepository;
import com.example.meditag.domain.member.entity.Member;
import com.example.meditag.domain.member.repository.MemberRepository;
import com.example.meditag.global.error.exception.CustomException;
import com.example.meditag.global.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlarmService {

    private final MemberRepository memberRepository;
    private final MedicineRepository medicineRepository;
    private final AlarmRepository alarmRepository;

    // 복용 여부 API
    @Transactional
    public AlarmResponseDTO toggleTaking (String username, Long medicineId, Long alarmId) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEDICINE_NOT_FOUND));

        Alarm alarm = alarmRepository.findById(alarmId)
                .orElseThrow(() -> new CustomException(ErrorCode.ALARM_NOT_FOUND));

        alarm.toggleTaking();

        alarmRepository.save(alarm);

        return AlarmMapper.toAlarmResponseDTO(alarm);
    }

    // 처방약 알림 만들기
    @Transactional
    public void createAlarmsForPrescribedMedicine(Medicine medicine, List<String> dosageTimes, List<LocalDateTime> alarmTimes) {
        // dosageTimes와 alarmTimes의 크기가 일치하는지 확인
        if (dosageTimes.size() != alarmTimes.size()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // dosageTimes와 alarmTimes에 대해 반복하면서 알람 생성
        for (int i = 0; i < dosageTimes.size(); i++) {
            String dosageTime = dosageTimes.get(i);
            LocalDateTime alarmTime = alarmTimes.get(i);

            Alarm alarm = AlarmMapper.toAlarmsForPrescribedMedicine(medicine, dosageTime, alarmTime);

            alarmRepository.save(alarm);  // 알람 저장
        }
    }

    // 일반약 알림 만들기
    @Transactional
    public void createAlarmsForNormalMedicine(Medicine medicine, List<LocalDateTime> alarmTimes) {
        for (LocalDateTime alarmTime : alarmTimes) {

            Alarm alarm = AlarmMapper.toAlarmsForNormalMedicine(medicine, alarmTime);

            alarmRepository.save(alarm);
        }
    }
}