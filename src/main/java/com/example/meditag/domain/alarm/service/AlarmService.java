package com.example.meditag.domain.alarm.service;

import com.example.meditag.domain.alarm.dto.response.AlarmResponseDTO;
import com.example.meditag.domain.alarm.entity.Alarm;
import com.example.meditag.domain.alarm.mapper.AlarmMapper;
import com.example.meditag.domain.alarm.repository.AlarmRepository;
import com.example.meditag.domain.calendar.entity.Calendar;
import com.example.meditag.domain.calendar.repository.CalendarRepository;
import com.example.meditag.domain.medicine.entity.Medicine;
import com.example.meditag.domain.medicine.repository.MedicineRepository;
import com.example.meditag.domain.member.entity.Guardian;
import com.example.meditag.domain.member.entity.Member;
import com.example.meditag.domain.member.repository.GuardianRepository;
import com.example.meditag.domain.member.repository.MemberRepository;
import com.example.meditag.global.error.exception.CustomException;
import com.example.meditag.global.error.exception.ErrorCode;
import com.example.meditag.global.sms.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmService {

    private final MemberRepository memberRepository;
    private final MedicineRepository medicineRepository;
    private final AlarmRepository alarmRepository;
    private final CalendarRepository calendarRepository;
    private final SmsService smsService;
    private final GuardianRepository guardianRepository;

    // 복용 여부 API
    @Transactional

    public AlarmResponseDTO toggleTakingByType(
            String username,
            Long medicineId,
            LocalDate date,
            String dosageTime,
            LocalDateTime alarmTime) {


        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEDICINE_NOT_FOUND));

        Calendar calendar = calendarRepository.findByMedicineAndDate(medicine, date)
                .orElseThrow(() -> new CustomException(ErrorCode.CALENDAR_NOT_FOUND));

        Alarm alarm;
        if (medicine.getPrescribed()) {
            if (dosageTime == null) throw new CustomException(ErrorCode.SAMPLE_ERROR);
            alarm = alarmRepository.findByCalendarAndDosageTime(calendar, dosageTime)
                    .orElseThrow(() -> new CustomException(ErrorCode.ALARM_NOT_FOUND));
        } else {
            if (alarmTime == null) throw new CustomException(ErrorCode.SAMPLE_ERROR);
            alarm = alarmRepository.findByCalendarAndAlarmTime(calendar, alarmTime)
                    .orElseThrow(() -> new CustomException(ErrorCode.ALARM_NOT_FOUND));
        }

        boolean previousState = alarm.isTaking();
        alarm.toggleTaking();

        // 복용 완료로 변경된 경우에만 SMS 발송
        if (!previousState && alarm.isTaking()) {

            sendNotificationToGuardians(member, medicine.getName());
        }

        alarmRepository.save(alarm);

        return AlarmMapper.toAlarmResponseDTO(alarm);
    }

    //sms 보내는 로직
    private void sendNotificationToGuardians(Member member, String medicineName) {
        List<Guardian> guardians = guardianRepository.findByMember(member);

        if (guardians.isEmpty()) {
            log.info("[AlarmService] 등록된 보호자가 없어 SMS 알림을 발송하지 않습니다 - 회원: {}",
                    member.getUsername());
            return;
        }

        for (Guardian guardian : guardians) {
            try {
                smsService.sendMedicationNotification(
                        guardian.getPhoneNumber(),
                        member.getName(),
                        medicineName);

                log.info("[AlarmService] 보호자({})에게 약 복용 알림 SMS 발송 완료 - 회원: {}, 약: {}",
                        guardian.getPhoneNumber(), member.getName(), medicineName);
            } catch (Exception e) {
                log.error("[AlarmService] 보호자({})에게 SMS 발송 실패: {}",
                        guardian.getPhoneNumber(), e.getMessage(), e);
            }
        }
    }
}