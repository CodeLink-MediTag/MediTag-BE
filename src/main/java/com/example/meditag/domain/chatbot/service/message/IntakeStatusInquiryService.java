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
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntakeStatusInquiryService {

    private final AlarmRepository alarmRepository;

    public boolean isApplicable(String message) {
        if (message == null || message.trim().isEmpty()) return false;
        String m = message.replaceAll("\\s+", "");

        // 사용자가 약의 주의사항, 부작용, 상호작용, 이유, 효능, 설명 등을 묻는 경우에는
        // 복용 현황 서비스가 아닌 GPTAnswerService로 처리해야 하므로 false를 반환합니다.
        String lowered = m.toLowerCase();
        if (lowered.contains("주의") || lowered.contains("주의사항") || lowered.contains("부작용")
                || lowered.contains("상호작용") || lowered.contains("이유") || lowered.contains("효능")
                || lowered.contains("설명")) {
            return false;
        }

        // 미복용/남은 약/안 먹은 약 패턴
        if (m.contains("아직") && m.contains("안") && m.contains("먹")) return true;
        if (m.contains("미복용")) return true;
        if ((m.contains("남은") && m.contains("약")) || m.contains("남은약")) return true;
        if (m.contains("안먹은약") || m.contains("안먹은")) return true;

        // 복용 완료/먹은 약 패턴
        if ((m.contains("먹은") && !m.contains("먹을")) || m.contains("복용한")) return true;

        // 복용 여부 질문형
        if (m.contains("먹었나") || m.contains("먹었니") || m.contains("먹었나요") || m.contains("먹었습니까")) return true;

        // 현황/상태/기록확인/체크 등
        if (m.contains("현황") || m.contains("상태") || m.contains("기록확인") || m.contains("체크") || m.contains("체크됐나")) return true;

        // "뭐 먹었" 또는 "무엇을 먹었"
        if (m.contains("뭐먹었") || m.contains("무엇을먹었")) return true;

        // 어제 복용 요약
        if (m.contains("어제") && (m.contains("먹었") || m.contains("복용"))) return true;

        return false;
    }

    public String execute(String username, String message) {
        try {
            final String msg = message == null ? "" : message.trim();

            // 1) 날짜 파싱
            LocalDate targetDate = parseDate(msg);

            // 2) 시간대 파싱
            Slot slot = parseSlot(msg);

            // 3) 상태 파싱
            Status status = parseStatus(msg);

            // 4) 어제 먹은 약 요약
            if (msg.replaceAll("\\s+", "").matches(".*(어제.*뭐.*먹었|어제.*무엇.*먹었|어제.*복용).*")) {
                LocalDate target = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
                List<Alarm> y = alarmRepository.findByDateOrderByAlarmTimeAsc(target);
                if (y == null || y.isEmpty()) return "어제 기록이 없어요.";
                String taken = y.stream().filter(Alarm::isTaking)
                        .map(this::safeName).distinct().collect(Collectors.joining(", "));
                if (taken.isBlank()) return "어제 복용한 약이 없어요.";
                return "어제 복용한 약: " + taken;
            }

            // 5) 복용 여부 질문형
            if (msg.replaceAll("\\s+", "").matches(".*(먹었나|먹었니|먹었나요|먹었습니까).*")) {
                String medicine = parseMedicineName(msg, slot);
                List<Alarm> alarms;
                if (slot != null) {
                    LocalDateTime start = LocalDateTime.of(targetDate, slot.start);
                    LocalDateTime end   = LocalDateTime.of(targetDate, slot.end);
                    if (medicine != null) {
                        alarms = alarmRepository.findByUsernameAndMedicineNameAndAlarmDate(username, medicine, start, end);
                    } else {
                        alarms = alarmRepository.findByUsernameAndAlarmDate(username, start, end);
                    }
                } else {
                    LocalDateTime start = targetDate.atStartOfDay();
                    LocalDateTime end   = targetDate.atTime(23,59,59);
                    if (medicine != null) {
                        alarms = alarmRepository.findByUsernameAndMedicineNameAndAlarmDate(username, medicine, start, end);
                    } else {
                        alarms = alarmRepository.findByUsernameAndDateTimeRange(username, start, end);
                    }
                }
                if (alarms == null || alarms.isEmpty()) {
                    return "해당 알람을 찾을 수 없어요.";
                }
                boolean takenAny = alarms.stream().anyMatch(Alarm::isTaking);
                return takenAny ? "네, 복용하셨어요." : "아니요, 아직 복용하지 않으셨어요.";
            }

            // 6) 상태별 응답
            if (status == Status.NOT_TAKEN) {
                return listStatus(username, targetDate, slot, false);
            } else if (status == Status.TAKEN) {
                return listStatus(username, targetDate, slot, true);
            } else {
                return summaryStatus(username, targetDate, slot);
            }
        } catch (Exception e) {
            log.error("IntakeStatusInquiryService error", e);
            return "복용 현황을 확인하는 중 오류가 발생했어요.";
        }
    }

    private LocalDate parseDate(String msg) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        if (msg == null) return today;
        String m = msg.replaceAll("\\s+", "");
        if (m.contains("어제")) return today.minusDays(1);
        if (m.contains("오늘")) return today;
        if (m.contains("내일")) return today.plusDays(1);
        if (m.contains("모레")) return today.plusDays(2);
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
        } catch (Exception ignored) {}
        return today;
    }

    private Status parseStatus(String msg) {
        if (msg == null) return null;
        String m = msg.replaceAll("\\s+", "");
        if ((m.contains("아직") && m.contains("안") && m.contains("먹")) || m.contains("미복용")
                || (m.contains("남은") && m.contains("약")) || m.contains("안먹은약") || m.contains("안먹은")) {
            return Status.NOT_TAKEN;
        }
        if ((m.contains("먹은") && !m.contains("먹을")) || m.contains("복용한")) {
            return Status.TAKEN;
        }
        if (m.contains("현황") || m.contains("상태") || m.contains("기록확인") || m.contains("체크") || m.contains("체크됐나")) {
            return Status.SUMMARY;
        }
        return null;
    }

    private String listStatus(String username, LocalDate date, Slot slot, boolean takenFlag) {
        List<Alarm> alarms;
        if (slot != null) {
            LocalDateTime start = LocalDateTime.of(date, slot.start);
            LocalDateTime end   = LocalDateTime.of(date, slot.end);
            alarms = alarmRepository.findByAlarmTimeBetweenOrderByAlarmTimeAsc(start, end);
        } else {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end   = date.atTime(23, 59, 59);
            alarms = alarmRepository.findByUsernameAndDateTimeRange(username, start, end);
        }
        if (alarms == null || alarms.isEmpty()) {
            if (slot != null) {
                return String.format("%d월 %d일 %s에는 약 일정이 없습니다.", date.getMonthValue(), date.getDayOfMonth(), slot.label);
            } else {
                return String.format("%d월 %d일에는 약 일정이 없습니다.", date.getMonthValue(), date.getDayOfMonth());
            }
        }
        List<Alarm> filtered = alarms.stream()
                .filter(a -> a.isTaking() == takenFlag)
                .collect(Collectors.toList());
        if (filtered.isEmpty()) {
            if (takenFlag) {
                if (slot != null) {
                    return String.format("%d월 %d일 %s에는 복용한 약이 없어요.", date.getMonthValue(), date.getDayOfMonth(), slot.label);
                } else {
                    return String.format("%d월 %d일에는 아직 복용한 약이 없어요.", date.getMonthValue(), date.getDayOfMonth());
                }
            } else {
                if (slot != null) {
                    return String.format("%d월 %d일 %s에는 아직 안 먹은 약이 없어요.", date.getMonthValue(), date.getDayOfMonth(), slot.label);
                } else {
                    return String.format("%d월 %d일에는 아직 안 먹은 약이 없어요.", date.getMonthValue(), date.getDayOfMonth());
                }
            }
        }
        String header;
        if (takenFlag) {
            header = slot != null
                    ? String.format("%d월 %d일 %s 복용한 약 목록입니다:\n", date.getMonthValue(), date.getDayOfMonth(), slot.label)
                    : String.format("%d월 %d일 복용한 약 목록입니다:\n", date.getMonthValue(), date.getDayOfMonth());
        } else {
            header = slot != null
                    ? String.format("%d월 %d일 %s 아직 안 먹은 약 목록입니다:\n", date.getMonthValue(), date.getDayOfMonth(), slot.label)
                    : String.format("%d월 %d일 아직 안 먹은 약 목록입니다:\n", date.getMonthValue(), date.getDayOfMonth());
        }
        String body = filtered.stream()
                .map(a -> String.format("%02d:%02d - %s", a.getAlarmTime().getHour(), a.getAlarmTime().getMinute(), safeName(a)))
                .collect(Collectors.joining("\n"));
        return header + body;
    }

    private String summaryStatus(String username, LocalDate date, Slot slot) {
        List<Alarm> alarms;
        if (slot != null) {
            LocalDateTime start = LocalDateTime.of(date, slot.start);
            LocalDateTime end   = LocalDateTime.of(date, slot.end);
            alarms = alarmRepository.findByAlarmTimeBetweenOrderByAlarmTimeAsc(start, end);
        } else {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end   = date.atTime(23, 59, 59);
            alarms = alarmRepository.findByUsernameAndDateTimeRange(username, start, end);
        }
        if (alarms == null || alarms.isEmpty()) {
            if (slot != null) {
                return String.format("%d월 %d일 %s에는 약 일정이 없습니다.", date.getMonthValue(), date.getDayOfMonth(), slot.label);
            } else {
                return String.format("%d월 %d일에는 약 일정이 없습니다.", date.getMonthValue(), date.getDayOfMonth());
            }
        }
        String header = slot != null
                ? String.format("%d월 %d일 %s 복용 현황입니다:\n", date.getMonthValue(), date.getDayOfMonth(), slot.label)
                : String.format("%d월 %d일 복용 현황입니다:\n", date.getMonthValue(), date.getDayOfMonth());
        String body = alarms.stream()
                .map(a -> String.format("%02d:%02d %s → %s", a.getAlarmTime().getHour(), a.getAlarmTime().getMinute(), safeName(a), a.isTaking() ? "복용 완료" : "미복용"))
                .collect(Collectors.joining("\n"));
        return header + body;
    }

    private enum Status {
        TAKEN,
        NOT_TAKEN,
        SUMMARY
    }

    private String safeName(Alarm a) {
        if (a.getCalendar() != null && a.getCalendar().getMedicine() != null) {
            return a.getCalendar().getMedicine().getName();
        }
        return "등록명 없음";
    }

    private String parseMedicineName(String msg, Slot slot) {
        String cleaned = msg.replaceAll("(오늘|어제|아침|점심|저녁|약|먹었나|먹었니|먹었어|먹음|복용|현황|상태|체크|기록|했어|해줘|\\?|\\s)", "");
        if (cleaned.isBlank()) return null;
        return cleaned;
    }

    private Slot parseSlot(String msg) {
        if (msg.contains("아침")) return Slot.MORNING;
        if (msg.contains("점심")) return Slot.NOON;
        if (msg.contains("저녁")) return Slot.EVENING;
        return null;
    }

    private enum Slot {
        MORNING("아침", LocalTime.of(3,0),  LocalTime.of(10,59)),
        NOON   ("점심", LocalTime.of(11,0), LocalTime.of(15,59)),
        EVENING("저녁", LocalTime.of(16,0), LocalTime.of(23,59));
        public final String label;
        public final LocalTime start, end;
        Slot(String l, LocalTime s, LocalTime e){label=l;start=s;end=e;}
    }
}
