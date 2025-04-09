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
        return message.matches(".*(복용|먹었|먹었어|상태).*변경.*") ||
                message.matches(".*(아침|점심|저녁|오전|오후).*약.*(먹었|완료|복용|머거|머었|먹거|먹겄|먹엇|).*");
    }

    public String execute(String username, String message) {
        // 1. 시간대 파악 프롬프트 생성 및 전송
        String prompt = """
        사용자가 약 복용 상태를 변경하고 싶어 합니다.
        '아침', '점심', '저녁' 중 어떤 시간대인지 하나만 알려주세요.
        애매하면 '없음'이라고 대답해주세요.
        그리고 오전 이면 아침으로 해주고 오후면 저녁으로 해줘.
     
        예시:
        - 아침약 먹었어 → 아침
        - 점심 약 복용 완료 → 점심
        - 저녁약 상태 바꿔줘 → 저녁
        - 약 먹었어 → 없음

        [사용자 메시지]: "%s"
        """.formatted(message);

        String timeResultRaw  = openAiService.sendMessageToOpenAi(prompt).trim();
        String timeResult = extractTimeKeyword(timeResultRaw);

        log.info("시간대 판단 결과: " + timeResult);

        // 2. 유효한 시간대인지 검사
        if (!List.of("아침", "점심", "저녁").contains(timeResult)) {
            return "어느 시간대의 약인지 잘 모르겠어요. '아침약 복용했어'처럼 말씀해주세요.";
        }

        // 3. 시간대별 범위 설정
        LocalDateTime[] timeRange = getTimeRange(timeResult);
        LocalDateTime start = timeRange[0];
        LocalDateTime end = timeRange[1];

        // 4. 오늘 날짜의 해당 시간대 알람 조회
        List<Alarm> alarms = alarmRepository.findByUsernameAndAlarmDate(
                username,
                start,
                end
        );

        // 5. 알람이 없는 경우 처리
        if (alarms.isEmpty()) {
            return "오늘 " + timeResult + " 시간대에는 복용할 약이 없어요.";
        }

        // 6. 복용 상태 변경 및 저장
        for (Alarm alarm : alarms) {
            alarm.toggleTaking(); // 복용 상태 변경
            alarmRepository.save(alarm); // 저장
        }

        // 7. 응답 메시지 구성
        return "오늘 " + timeResult + "약의 복용 상태를 변경했어요! 잘하셨어요!";
    }

    private LocalDateTime[] getTimeRange(String timeKeyword) {
        LocalDate today = LocalDate.now();
        switch (timeKeyword) {
            case "아침":
                return new LocalDateTime[]{ today.atTime(3, 0), today.atTime(10, 59) };
            case "점심":
                return new LocalDateTime[]{ today.atTime(11, 0), today.atTime(15, 59) };
            case "저녁":
                return new LocalDateTime[]{ today.atTime(16, 0), today.atTime(23, 59) };
            default:
                throw new IllegalArgumentException("유효하지 않은 시간대입니다: " + timeKeyword);
        }
    }

    private String extractTimeKeyword(String response) {
        if (response.contains("아침")) return "아침";
        if (response.contains("점심")) return "점심";
        if (response.contains("저녁")) return "저녁";
        return "없음";
    }

}
