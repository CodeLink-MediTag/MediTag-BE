package com.example.meditag.domain.chatbot.service.message;

import com.example.meditag.domain.alarm.entity.Alarm;
import com.example.meditag.domain.alarm.repository.AlarmRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DateMedicineInquiryService {

    private final AlarmRepository alarmRepository;

    public String execute(String username, String message) {
        try {
            String msg = message == null ? "" : message;
            // 1) 날짜 파싱 (오늘/내일/모레/특정 날짜), 기본은 오늘
            LocalDate targetDate = parseDate(msg);

            // 2) 시간대 파싱 (아침/점심/저녁)
            String slot = parseSlot(msg);

            // 3) 시간 범위 계산
            if (slot != null) {
                // 시간대가 지정된 경우
                LocalDateTime[] range = getTimeRangeForSlot(targetDate, slot);
                List<Alarm> alarms = alarmRepository.findByUsernameAndDateTimeRange(
                        username, range[0], range[1]);
                return formatResponse(targetDate, slot, alarms);
            } else {
                // 시간대가 지정되지 않은 경우 전체 일정 조회
                LocalDateTime start = targetDate.atStartOfDay();
                LocalDateTime end = targetDate.atTime(23, 59, 59);
                List<Alarm> alarms = alarmRepository.findByUsernameAndDateTimeRange(username, start, end);
                return formatResponse(targetDate, null, alarms);
            }
        } catch (Exception e) {
            log.error("[DateMedicineInquiryService] error: {}", e.getMessage(), e);
            return "약 정보를 불러오는 중 문제가 발생했어요. 잠시 후 다시 시도해 주세요.";
        }
    }

    private LocalDate parseDate(String msg) {
        if (msg == null) return LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        String m = msg.replaceAll("\\s+", "");
        if (m.contains("오늘")) {
            return today;
        }
        if (m.contains("내일")) {
            return today.plusDays(1);
        }
        if (m.contains("모레")) {
            return today.plusDays(2);
        }
        try {
            java.util.regex.Matcher md = java.util.regex.Pattern.compile("(\\d{1,2})월\\s*(\\d{1,2})일").matcher(m);
            if (md.find()) {
                int month = Integer.parseInt(md.group(1));
                int day = Integer.parseInt(md.group(2));
                int year = today.getYear();
                java.util.regex.Matcher yd = java.util.regex.Pattern.compile("(\\d{4})년").matcher(m);
                if (yd.find()) {
                    year = Integer.parseInt(yd.group(1));
                }
                return LocalDate.of(year, month, day);
            }
        } catch (Exception ignored) {
        }
        return today;
    }

    private String parseSlot(String msg) {
        if (msg == null) return null;
        if (msg.contains("아침")) return "아침";
        if (msg.contains("점심")) return "점심";
        if (msg.contains("저녁")) return "저녁";
        return null;
    }

    private LocalDateTime[] getTimeRangeForSlot(LocalDate date, String slot) {
        return switch (slot) {
            case "아침" -> new LocalDateTime[] { date.atTime(3, 0), date.atTime(10, 59) };
            case "점심" -> new LocalDateTime[] { date.atTime(11, 0), date.atTime(15, 59) };
            case "저녁" -> new LocalDateTime[] { date.atTime(16, 0), date.atTime(23, 59) };
            default -> new LocalDateTime[] { date.atStartOfDay(), date.atTime(23, 59, 59) };
        };
    }

    private String formatResponse(LocalDate date, String slot, List<Alarm> alarms) {
        if (alarms == null || alarms.isEmpty()) {
            if (slot != null) {
                return String.format("%d월 %d일 %s에는 약 일정이 없습니다.",
                        date.getMonthValue(), date.getDayOfMonth(), slot);
            } else {
                return String.format("%d월 %d일에는 약 일정이 없습니다.",
                        date.getMonthValue(), date.getDayOfMonth());
            }
        }

        String header = (slot != null)
                ? String.format("%d월 %d일 %s 복용 일정입니다:\n",
                date.getMonthValue(), date.getDayOfMonth(), slot)
                : String.format("%d월 %d일 복용 일정입니다:\n",
                date.getMonthValue(), date.getDayOfMonth());

        String body = alarms.stream()
                .sorted(Comparator.comparing(Alarm::getAlarmTime))
                .map(a -> String.format("%02d:%02d - %s",
                        a.getAlarmTime().getHour(),
                        a.getAlarmTime().getMinute(),
                        a.getCalendar().getMedicine().getName()))
                .collect(Collectors.joining("\n"));

        return header + body + "\n이 약들을 확인해 주세요.";
    }
}
