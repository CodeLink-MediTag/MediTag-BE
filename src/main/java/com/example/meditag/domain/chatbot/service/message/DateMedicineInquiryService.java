package com.example.meditag.domain.chatbot.service.message;

import com.example.meditag.domain.alarm.entity.Alarm;
import com.example.meditag.domain.alarm.repository.AlarmRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * "오늘/특정 날짜에 먹을 약 뭐야?" 질의 처리
 * - 등록/수정 이후에도 항상 DB 기준으로 재조회
 * - 파싱/조회 오류 발생 시 사용자에게 안전한 안내 문구 반환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DateMedicineInquiryService {

    private final AlarmRepository alarmRepository;

    private static final DateTimeFormatter KOR = DateTimeFormatter.ofPattern("M월 d일");

    /** 간단 키워드 감지 */
    public boolean isApplicable(String message) {
        if (message == null) return false;
        String m = message.replaceAll("\\s+", "");
        return m.matches(".*((오늘|지금|내일|어제|모레|그제)|\\d{1,2}월\\d{1,2}일).*(약|복용).*(뭐|뭐야|있어|알려줘|확인|현황|리스트).*");
    }

    /** 핵심 로직 */
    @Transactional(readOnly = true)
    public String execute(String username, String message) {
        try {
            LocalDate date = parseDateOrToday(message);

            // AlarmRepository 시그니처에 맞춰 '하루 범위'로 조회
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(23, 59, 59);
            List<Alarm> alarms = alarmRepository.findByUsernameAndAlarmDate(username, start, end);

            if (alarms == null || alarms.isEmpty()) {
                return KOR.format(date) + "에는 복용할 약이 없습니다.";
            }

            String body = alarms.stream()
                    .sorted(Comparator.comparing(Alarm::getAlarmTime))
                    .map(a -> String.format("%s - %s",
                            safeTime(a),
                            safeMedicineName(a)))
                    .collect(Collectors.joining("\n"));

            return String.format("%s 복용 일정입니다:\n%s", KOR.format(date), body);

        } catch (Exception e) {
            log.error("DateMedicineInquiryService.execute error", e);
            return "지금 복용 일정을 확인하는 중 문제가 생겼어요. 날짜를 바꿔 다시 물어보시거나, ‘오늘 복용 현황 알려줘’라고 말씀해 주세요.";
        }
    }

    // ===== 내부 유틸 =====

    private LocalDate parseDateOrToday(String msg) {
        if (msg == null) return LocalDate.now();
        if (msg.contains("오늘") || msg.contains("지금")) return LocalDate.now();
        if (msg.contains("내일")) return LocalDate.now().plusDays(1);
        if (msg.contains("모레")) return LocalDate.now().plusDays(2);
        if (msg.contains("글피")) return LocalDate.now().plusDays(3);
        if (msg.contains("어제")) return LocalDate.now().minusDays(1);
        if (msg.contains("그제")) return LocalDate.now().minusDays(2);

        Matcher m = Pattern.compile("(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일").matcher(msg);
        if (m.find()) {
            int month = Integer.parseInt(m.group(1));
            int day = Integer.parseInt(m.group(2));
            int year = LocalDate.now().getYear();
            return LocalDate.of(year, month, day);
        }

        // 파싱 실패 → 오늘
        return LocalDate.now();
    }

    private String safeTime(Alarm a) {
        try {
            return a.getAlarmTime() != null ? a.getAlarmTime().toLocalTime().toString() : "-";
        } catch (Exception e) {
            return "-";
        }
    }

    private String safeMedicineName(Alarm a) {
        try {
            return a.getCalendar() != null
                    && a.getCalendar().getMedicine() != null
                    ? a.getCalendar().getMedicine().getName()
                    : "약 이름 미상";
        } catch (Exception e) {
            return "약 이름 미상";
        }
    }
}
