package com.example.meditag.domain.chatbot.service.message;

import com.example.meditag.domain.alarm.entity.Alarm;
import com.example.meditag.domain.alarm.repository.AlarmRepository;
import com.example.meditag.domain.chatbot.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DosageToggleService {

    private final OpenAiService openAiService;
    private final AlarmRepository alarmRepository;

    // “약 먹었어/복용했어 …” 류만 처리. 질문형/현황형은 제외하여 조회 서비스로 보냄
    public boolean isApplicable(String message) {
        if (message == null) return false;
        String msg = message.trim();

        // 질문형/현황형 → false
        if (msg.endsWith("?") || msg.matches(".*(먹었나|먹었니|먹었나요|먹었습니까|현황|상태).*")) {
            return false;
        }
        // 확정/요청형 표현
        return msg.matches(".*(먹었어|먹음|복용했어|복용완료|체크해줘|변경해줘|완료로해줘|체크했어|기록해줘).*");
    }

    // 규칙 기반 1차 추출
    private static final Pattern SLOT_PAT = Pattern.compile("(아침|점심|저녁)");
    // 사전(예시) — 실제론 사용자 등록 약명 목록으로 대체 추천
    private static final Pattern MEDI_PAT = Pattern.compile("(탈모약|타이레놀|이부프로펜|멕시펜|철분|비타민|오메가3|혈압약|위장약|감기약|감기|두통약|두통)");

    private static String normalize(String s) {
        return s == null ? "" : s.toLowerCase(Locale.KOREAN).trim().replaceAll("\\s+", "");
    }

    @Transactional
    public String execute(String username, String message) {
        final String raw = message == null ? "" : message.trim();
        final String compact = normalize(raw);

        // 1) 날짜 파싱 (오늘/내일/모레/특정 날짜). 기본은 오늘
        LocalDate targetDate = parseDate(raw);

        // 2) 시간대 및 약명 추출
        String timeSlot = extractSlotByRule(compact).orElse("없음");
        String mediName = extractMedicineByRule(raw).orElse("없음");

        // 3) 필요하면 GPT 보조 파싱
        if ("없음".equals(timeSlot) || needsMediHelp(compact, mediName)) {
            Parsed parsed = askGptForParse(raw);
            if (parsed.timeSlot != null) timeSlot = parsed.timeSlot;
            if (parsed.mediName != null) mediName = parsed.mediName;
        }

        // 4) 시간대가 없으면 먼저 시간대부터 요청
        if (!("아침".equals(timeSlot) || "점심".equals(timeSlot) || "저녁".equals(timeSlot))) {
            return "어느 시간대의 약인지 알려주세요. 아침, 점심, 저녁 중 하나를 말씀해주시면 기록할게요.";
        }

        // 5) 약 이름이 없다면 → 이름 요청 (이전처럼 '알람이 없어요'라고 끊지 않음)
        if ("없음".equals(mediName)) {
            String dateStr = String.format("%d월 %d일", targetDate.getMonthValue(), targetDate.getDayOfMonth());
            // 해당 시간대에 예정된 약이 있으면 후보도 같이 제시
            List<String> candidates = listMedicineCandidates(username, targetDate, timeSlot);
            String tip = candidates.isEmpty()
                    ? ""
                    : "\n예: " + String.join(", ", candidates);
            return String.format("%s %s 시간대에 어떤 약을 복용하셨나요? 약 이름을 알려주시면 기록해 드릴게요 %s",
                    dateStr, timeSlot, tip);
        }

        // 6) 시간 범위 계산
        LocalDateTime[] range = getTimeRangeKSTForDate(targetDate, timeSlot);
        LocalDateTime start = range[0];
        LocalDateTime end = range[1];

        // 7) 약명 매칭: '약' 유무 모두 시도 (정확/변형 매칭)
        //    - 사용자는 '감기' 또는 '감기약'이라 말할 수 있으므로 둘 다 시도
        String given = mediName.trim();
        String withYak = given.endsWith("약") ? given : given + "약";
        String withoutYak = given.endsWith("약") ? given.substring(0, given.length() - 1) : given;

        List<Alarm> alarms = findByExactOrVariant(username, given, withYak, withoutYak, start, end);

        // 8) 매칭 실패 시: 후보 보여주고 이름 다시 요청
        if (alarms == null || alarms.isEmpty()) {
            List<String> candidates = listMedicineCandidates(username, targetDate, timeSlot);
            String tip = candidates.isEmpty()
                    ? "해당 시간대 복용 알람을 찾을 수 없어요."
                    : "해당 시간대 약 목록: " + String.join(", ", candidates);
            return String.format("%d월 %d일 %s 시간대에 '%s' 약 알람이 없어요.\n%s\n정확한 약 이름을 알려주시면 기록할게요",
                    targetDate.getMonthValue(), targetDate.getDayOfMonth(), timeSlot, given, tip);
        }

        // 9) 상태 토글
        for (Alarm alarm : alarms) {
            try { alarm.toggleTaking(); } catch (Exception ignore) {}
        }

        // 10) 응답: 표시용 이름은 '약' 제거해 자연스럽게
        String displayName = withoutYak.isBlank() ? given : withoutYak;
        String dateStr = String.format("%d월 %d일", targetDate.getMonthValue(), targetDate.getDayOfMonth());
        return String.format("%s %s '%s'약의 복용 상태를 변경했어요! 잘하셨어요",
                dateStr, timeSlot, displayName);
    }

    /* ================= helpers ================= */

    private Optional<String> extractSlotByRule(String compact) {
        Matcher m = SLOT_PAT.matcher(compact);
        if (m.find()) return Optional.of(m.group(1));
        return Optional.empty();
    }

    private Optional<String> extractMedicineByRule(String raw) {
        Matcher m = MEDI_PAT.matcher(raw);
        if (m.find()) {
            String name = m.group(1);
            // 사용자가 '감기'라고만 말한 경우도 그대로 허용
            return Optional.ofNullable(name);
        }
        return Optional.empty();
    }

    private boolean needsMediHelp(String compact, String mediName) {
        return "없음".equals(mediName) && compact.contains("약");
    }

    private Parsed askGptForParse(String user) {
        String prompt = """
            아래 문장에서 복용 시간대와 약 이름을 추출해줘.
            반드시 이 형식만 출력해:
            시간대:[아침|점심|저녁|없음],약:[약이름|없음]

            예)
            - "점심에 타이레놀 먹었어" -> 시간대:[점심],약:[타이레놀]
            - "아침에 약 먹었어" -> 시간대:[아침],약:[없음]
            - "약 먹었어" -> 시간대:[없음],약:[없음]

            문장: "%s"
            """.formatted(user);

        String resp = "";
        try { resp = openAiService.sendMessageToOpenAi(prompt); }
        catch (Exception e) { log.warn("OpenAI parse failed: {}", e.toString()); }
        if (resp == null) return new Parsed(null, null);
        log.info("GPT 파싱 응답: {}", resp.trim());

        Pattern p = Pattern.compile("시간대:\\[(아침|점심|저녁|없음)\\]\\s*,?\\s*약:\\[(.*?)\\]");
        Matcher m = p.matcher(resp);
        if (m.find()) {
            String slot = m.group(1);
            String name = m.group(2).trim();
            name = name.replaceAll("[^가-힣a-zA-Z0-9]", "");
            if (name.isEmpty()) name = "없음";
            return new Parsed(slot, name);
        }
        return new Parsed(null, null);
    }

    private static class Parsed {
        final String timeSlot;
        final String mediName;
        Parsed(String t, String m) { this.timeSlot = t; this.mediName = m; }
    }

    private LocalDateTime[] getTimeRangeKSTForDate(LocalDate date, String slot) {
        return switch (slot) {
            case "아침" -> new LocalDateTime[]{ date.atTime(3, 0),  date.atTime(10, 59) };
            case "점심" -> new LocalDateTime[]{ date.atTime(11, 0), date.atTime(15, 59) };
            case "저녁" -> new LocalDateTime[]{ date.atTime(16, 0), date.atTime(23, 59) };
            default -> throw new IllegalArgumentException("유효하지 않은 시간대입니다: " + slot);
        };
    }

    private LocalDate parseDate(String msg) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        if (msg == null) return today;
        String m = msg.replaceAll("\\s+", "");
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
                if (yd.find()) year = Integer.parseInt(yd.group(1));
                return LocalDate.of(year, month, day);
            }
        } catch (Exception ignored) {}
        return today;
    }

    private List<Alarm> findByExactOrVariant(
            String username, String given, String withYak, String withoutYak,
            LocalDateTime start, LocalDateTime end
    ) {
        // 1) 그대로
        List<Alarm> res = alarmRepository.findByUsernameAndMedicineNameAndAlarmDate(username, given, start, end);
        if (res != null && !res.isEmpty()) return res;

        // 2) with '약'
        if (!withYak.equals(given)) {
            res = alarmRepository.findByUsernameAndMedicineNameAndAlarmDate(username, withYak, start, end);
            if (res != null && !res.isEmpty()) return res;
        }

        // 3) without '약'
        if (!withoutYak.equals(given)) {
            res = alarmRepository.findByUsernameAndMedicineNameAndAlarmDate(username, withoutYak, start, end);
            if (res != null && !res.isEmpty()) return res;
        }

        // 4) 최후수단: 해당 시간대 전체에서 이름이 '포함'되는 것(레포지토리에 지원 메서드가 없다면 생략)
        //   - 해당 프로젝트에 contains 검색 메서드가 없다면 빼세요.
        try {
            List<Alarm> all = alarmRepository.findByUsernameAndAlarmDate(username, start, end);
            if (all != null) {
                String g = given;
                return all.stream()
                        .filter(a -> {
                            String n = a.getCalendar().getMedicine().getName();
                            return n != null && (n.contains(g) || n.contains(withYak) || n.contains(withoutYak));
                        })
                        .collect(Collectors.toList());
            }
        } catch (Exception ignore) {}

        return Collections.emptyList();
    }

    private List<String> listMedicineCandidates(String username, LocalDate date, String slot) {
        LocalDateTime[] range = getTimeRangeKSTForDate(date, slot);
        List<Alarm> all = alarmRepository.findByUsernameAndAlarmDate(username, range[0], range[1]);
        if (all == null) return Collections.emptyList();
        return all.stream()
                .map(a -> {
                    String n = a.getCalendar() != null && a.getCalendar().getMedicine() != null
                            ? a.getCalendar().getMedicine().getName() : null;
                    if (n == null) return null;
                    // 후보 표시에서는 '약'을 떼서 자연스럽게
                    return n.endsWith("약") ? n.substring(0, n.length() - 1) : n;
                })
                .filter(Objects::nonNull)
                .distinct()
                .limit(5)
                .collect(Collectors.toList());
    }
}
