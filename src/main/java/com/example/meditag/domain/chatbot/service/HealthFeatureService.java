package com.example.meditag.domain.chatbot.service;

import com.example.meditag.domain.alarm.entity.Alarm;
import com.example.meditag.domain.alarm.repository.AlarmRepository;
import com.example.meditag.domain.chatbot.dto.DailyUserStatus;
import com.example.meditag.domain.member.entity.Member;
import com.example.meditag.domain.member.repository.MemberRepository;
import com.example.meditag.domain.record.entity.Recording;
import com.example.meditag.domain.record.repository.RecordRepository;
import com.example.meditag.domain.medicine.dto.response.MedicineGetDateResponseDTO;
import com.example.meditag.domain.medicine.service.MedicineService;
import com.example.meditag.global.error.exception.CustomException;
import com.example.meditag.global.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HealthFeatureService {

    private final MemberRepository memberRepository;
    private final AlarmRepository alarmRepository;
    private final RecordRepository recordingRepository;
    private final MedicineService medicineService;

    /**
     * 1. 복용 상태 토글
     */
    public void toggleTaking(Long alarmId) {
        Alarm alarm = alarmRepository.findById(alarmId)
                .orElseThrow(() -> new CustomException(ErrorCode.ALARM_NOT_FOUND));
        alarm.toggleTaking();
        alarmRepository.save(alarm);
    }

    /**
     * 2. 날짜 지정 질문 대응 - 챗봇 호출용 데이터 생성
     */
    public DailyUserStatus getStatusByDate(String username, LocalDate date) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        MedicineGetDateResponseDTO medicineData = medicineService.getMedicinesByDate(username, date.toString());
        List<Recording> recordings = recordingRepository.findByMemberOrderByRecordingTimeDesc(member);

        return DailyUserStatus.builder()
                .date(date.toString())
                .medicines(medicineData.getMedicines())
                .recordings(recordings)
                .build();
    }

//    /**
//     * 3. 복용 예정 시간 알림 푸시 (매분마다 스케줄링 예시)
//     */
//    @Scheduled(cron = "0 * * * * *") // 매분 실행
//    public void pushUpcomingAlarms() {
//        LocalDateTime now = LocalDateTime.now();
//        LocalDateTime rangeStart = now;
//        LocalDateTime rangeEnd = now.plusMinutes(1);
//
//        List<Alarm> upcomingAlarms = alarmRepository.findByAlarmTimeBetween(rangeStart, rangeEnd);
//
//        for (Alarm alarm : upcomingAlarms) {
//            Member member = alarm.getCalendar().getMedicine().getMember();
//            String message = "곧 복용해야 할 약이 있어요: " + alarm.getCalendar().getMedicine().getName()
//                    + " (시간: " + alarm.getAlarmTime() + ")";
//
//            // 알림 전송 로직 (예: FCM 또는 SMS)
//            sendNotification(member, message);
//        }
//    }

    private void sendNotification(Member member, String message) {
        // FCM, Firebase, 문자 API 등과 연동
        System.out.println("[푸시 알림] " + member.getUsername() + " → " + message);
    }
}
