package com.example.meditag.domain.chatbot.service.message;

import com.example.meditag.domain.alarm.entity.Alarm;
import com.example.meditag.domain.alarm.repository.AlarmRepository;
import com.example.meditag.domain.chatbot.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class DosageToggleService {

    private final OpenAiService openAiService;
    private final AlarmRepository alarmRepository;

    // ⚠️ 토글을 강하게 시사하는 키워드만 남기고, 모호한 건 상위 라우팅에서 처리
    public boolean isApplicable(String message) {
        String m = normalize(message);
        return m.matches(".*(복용했|먹었어|먹음|복용완료|기록해줘|처리해줘|체크해줘).*");
    }

    // ====== 규칙 기반 1차 추출: 빠르고 안정적 ======
    private static final Pattern SLOT_PAT = Pattern.compile("(아침|점심|저녁)");
    private static final Pattern MEDI_PAT = Pattern.compile("(타이레놀|이부프로펜|멕시펜|철분|비타민|오메가3|혈압약|위장약|감기약|두통약)");

    private static String normalize(String s) {
        return s == null ? "" : s.toLowerCase(Locale.KOREAN).trim().replaceAll("\\s+", "");
    }

    @Transactional
    public String execute(String username, String message) {
        // 0) 규칙 기반 1차 추출
        String raw = message == null ? "" : message.trim();
        String compact = normalize(raw);

        String timeSlot = extractSlotByRule(compact).orElse("없음");
        String mediName = extractMedicineByRule(raw).orElse("없음");

        // 1) 규칙만으로 부족하면 GPT 보조
        if ("없음".equals(timeSlot) || needsMediHelp(compact, mediName)) {
            Parsed parsed = askGptForParse(raw);
            if (parsed.timeSlot != null) timeSlot = parsed.timeSlot;
            if (parsed.mediName != null) mediName = parsed.mediName;
        }

        // 2) 시간대 검증
        if (!("아침".equals(timeSlot) || "점심".equals(timeSlot) || "저녁".equals(timeSlot))) {
            return "어느 시간대의 약인지 잘 모르겠어요. '아침약 복용했어'처럼 말씀해주세요.";
        }

        // 3) 시간대 → 시간 범위 (KST 고정)
        LocalDateTime[] range = getTimeRangeKST(timeSlot);
        LocalDateTime start = range[0];
        LocalDateTime end = range[1];

        // 4) 알람 조회
        List<Alarm> alarms = "없음".equals(mediName)
                ? alarmRepository.findByUsernameAndAlarmDate(username, start, end)
                : alarmRepository.findByUsernameAndMedicineNameAndAlarmDate(username, mediName, start, end);

        if (alarms.isEmpty()) {
            return String.format("오늘 %s 시간대에 '%s' 복용 알람이 없어요.",
                    timeSlot, "없음".equals(mediName) ? "해당" : mediName);
        }

        // 5) 토글 수행 (트랜잭션 내)
        for (Alarm alarm : alarms) {
            alarm.toggleTaking();
            // save() 불필요할 수 있으나, Repository 구현에 따라 유지
            // alarmRepository.save(alarm);
        }

        return String.format("오늘 %s %s 약의 복용 상태를 변경했어요! 잘하셨어요!",
                timeSlot, "없음".equals(mediName) ? "모든" : "'" + mediName + "'");
    }

    private Optional<String> extractSlotByRule(String compact) {
        Matcher m = SLOT_PAT.matcher(compact);
        if (m.find()) return Optional.of(m.group(1));
        // “지금” + 현재 시각 근접 슬롯 추정 로직을 넣고 싶다면 여기에서 처리 가능
        return Optional.empty();
    }

    private Optional<String> extractMedicineByRule(String raw) {
        // 간단: 사전 등록된 약명만 우선 매칭(실제로는 사용자 등록 약명 목록을 조회해 매칭 권장)
        Matcher m = MEDI_PAT.matcher(raw);
        if (m.find()) return Optional.of(m.group(1));
        return Optional.empty();
    }

    private boolean needsMediHelp(String compact, String mediName) {
        // "약" 언급만 있고 구체명 없음 → GPT 도움
        return "없음".equals(mediName) && compact.contains("약");
    }

    // ====== GPT 보조 파서: 엄격한 포맷 요구 + 정규식 파싱 ======
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
        try {
            resp = openAiService.sendMessageToOpenAi(prompt).trim();
        } catch (Exception e) {
            log.warn("OpenAI parse failed: {}", e.toString());
        }
        log.info("GPT 응답: {}", resp);

        // 정규식으로 안전하게 파싱
        Pattern p = Pattern.compile("시간대:\\[(아침|점심|저녁|없음)\\]\\s*,?\\s*약:\\[(.*?)\\]");
        Matcher m = p.matcher(resp);
        if (m.find()) {
            String slot = m.group(1);
            String name = m.group(2).trim();
            name = name.replaceAll("[^가-힣a-zA-Z0-9]", "");
            if (name.isEmpty()) name = "없음";
            return new Parsed(slot, name);
        }
        // 실패 시 null 반환 → 상위 로직에서 기존 값 유지
        return new Parsed(null, null);
    }

    private static class Parsed {
        final String timeSlot;  // null 가능
        final String mediName;  // null 가능
        Parsed(String t, String m) { this.timeSlot = t; this.mediName = m; }
    }

    private LocalDateTime[] getTimeRangeKST(String slot) {
        ZoneId zone = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(zone);
        return switch (slot) {
            case "아침" -> new LocalDateTime[]{ today.atTime(3, 0), today.atTime(10, 59) };
            case "점심" -> new LocalDateTime[]{ today.atTime(11, 0), today.atTime(15, 59) };
            case "저녁" -> new LocalDateTime[]{ today.atTime(16, 0), today.atTime(23, 59) };
            default -> throw new IllegalArgumentException("유효하지 않은 시간대입니다: " + slot);
        };
    }
}
