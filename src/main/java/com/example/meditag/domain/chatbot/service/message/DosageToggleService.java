package com.example.meditag.domain.chatbot.service.message;

import com.example.meditag.domain.alarm.entity.Alarm;
import com.example.meditag.domain.alarm.repository.AlarmRepository;
import com.example.meditag.domain.chatbot.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DosageToggleService {

    private final OpenAiService openAiService;
    private final AlarmRepository alarmRepository;

    public boolean isApplicable(String message) {
        return message.matches(".*(복용|먹었|먹었어|먹음|복용했|상태|변경|머거|머었|먹겄|먹엇|먹거).*");
    }

    public String execute(String username, String message) {
        // 1. GPT에 시간대 + 약 이름 추출 요청
        String prompt = """
            사용자가 특정 시간대에 어떤 약을 복용했다고 말했어요.
            문장에서 시간대(아침/점심/저녁)와 약 이름을 각각 추출해서 알려줘.
            형식: 시간대: [아침|점심|저녁|없음], 약: [약이름|없음]

            예시:
            - 점심에 타이레놀 먹었어 → 시간대: 점심, 약: 타이레놀
            - 아침에 약 먹었어 → 시간대: 아침, 약: 없음
            - 약 먹었어 → 시간대: 없음, 약: 없음

            사용자 메시지: "%s"
            """.formatted(message);

        String gptResponse = openAiService.sendMessageToOpenAi(prompt).trim();
        log.info("GPT 응답: " + gptResponse);

        String timeResult = extractTimeKeyword(gptResponse);
        String medicineName = extractMedicineName(gptResponse);

        // 시간대 유효성 검사
        if (!List.of("아침", "점심", "저녁").contains(timeResult)) {
            return "어느 시간대의 약인지 잘 모르겠어요. '아침약 복용했어'처럼 말씀해주세요.";
        }

        // 시간대 → 시간 범위
        LocalDateTime[] timeRange = getTimeRange(timeResult);
        LocalDateTime start = timeRange[0];
        LocalDateTime end = timeRange[1];

        // 약 이름 조건에 따라 알람 조회
        List<Alarm> alarms;
        if (medicineName.equals("없음")) {
            alarms = alarmRepository.findByUsernameAndAlarmDate(username, start, end);
        } else {
            alarms = alarmRepository.findByUsernameAndMedicineNameAndAlarmDate(username, medicineName, start, end);
        }

        if (alarms.isEmpty()) {
            return String.format("오늘 %s 시간대에 '%s' 복용 알람이 없어요.",
                    timeResult, medicineName.equals("없음") ? "해당" : medicineName);
        }

        for (Alarm alarm : alarms) {
            alarm.toggleTaking();
            alarmRepository.save(alarm);
        }

        return String.format("오늘 %s %s 약의 복용 상태를 변경했어요! 잘하셨어요!",
                timeResult,
                medicineName.equals("없음") ? "모든" : "'" + medicineName + "'");
    }

    private LocalDateTime[] getTimeRange(String timeKeyword) {
        LocalDate today = LocalDate.now();
        return switch (timeKeyword) {
            case "아침" -> new LocalDateTime[]{ today.atTime(3, 0), today.atTime(10, 59) };
            case "점심" -> new LocalDateTime[]{ today.atTime(11, 0), today.atTime(15, 59) };
            case "저녁" -> new LocalDateTime[]{ today.atTime(16, 0), today.atTime(23, 59) };
            default -> throw new IllegalArgumentException("유효하지 않은 시간대입니다: " + timeKeyword);
        };
    }

    private String extractTimeKeyword(String gptResponse) {
        if (gptResponse.contains("아침")) return "아침";
        if (gptResponse.contains("점심")) return "점심";
        if (gptResponse.contains("저녁")) return "저녁";
        return "없음";
    }

    private String extractMedicineName(String gptResponse) {
        if (gptResponse.contains("약: 없음")) return "없음";
        int idx = gptResponse.indexOf("약:");
        if (idx != -1) {
            String name = gptResponse.substring(idx + 3).trim();
            name = name.replaceAll("[^가-힣a-zA-Z0-9]", ""); // 한글, 영문, 숫자만
            return name.isEmpty() ? "없음" : name;
        }
        return "없음";
    }
}