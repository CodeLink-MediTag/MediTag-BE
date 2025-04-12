package com.example.meditag.domain.chatbot.service.message;

import com.example.meditag.domain.alarm.entity.Alarm;
import com.example.meditag.domain.alarm.repository.AlarmRepository;
import com.example.meditag.domain.chatbot.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class DateMedicineInquiryService {

    private final AlarmRepository alarmRepository;
    private final OpenAiService openAiService;

    public boolean isApplicable(String message) {
        // 날짜 패턴 또는 상대 날짜 표현 인식
        return message.matches(".*((\\d{1,2})월 (\\d{1,2})일|오늘|내일|모레|글피|어제|그제).*약.*");
    }

    public String execute(String username, String message) {
        LocalDate date = extractDate(message);

        // 1. 해당 날짜의 알람 조회
        List<Alarm> alarms = alarmRepository.findByUsernameAndAlarmDate(
                username,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay().minusSeconds(1)
        );

        // 2. 알람이 없는 경우 처리
        if (alarms.isEmpty()) {
            return date.getMonthValue() + "월 " + date.getDayOfMonth() + "일에는 복용할 약이 없습니다.";
        }

        // 3. 프롬프트 생성
        StringBuilder prompt = new StringBuilder();
        prompt.append("너는 사용자의 복약을 도와주는 건강한 AI 친구야.\n")
                .append(date.getMonthValue()).append("월 ").append(date.getDayOfMonth()).append("일 복약 일정이야.\n")
                .append("날짜 앞에 오늘 쓰지말아줘.그리고 앞에 대답하지말아줘.\n")
                .append("중복 표현 없이 간결하게 해줘. 복용 안 한 약과 복용한 약을 자연스럽게 말해줘.\n")
                .append("그리고 살짝 따뜻하게 말해줘. 그리고 존댓말로 해줘 \n")
                .append("\n");

        for (Alarm alarm : alarms.stream().sorted(Comparator.comparing(Alarm::getAlarmTime)).toList()) {
            prompt.append("- 약: ").append(alarm.getCalendar().getMedicine().getName())
                    .append(" / 시간: ").append(alarm.getAlarmTime().toLocalTime())
                    .append(" / 상태: ").append(alarm.isTaking() ? "복용 완료" : "복용 전")
                    .append("\n");
        }

        // 4. GPT에 전송하고 응답 반환
        return openAiService.sendMessageToOpenAi(prompt.toString());
    }

    private LocalDate extractDate(String message) {
        // 명시적 날짜 처리
        Matcher matcher = Pattern.compile("(\\d{1,2})월 (\\d{1,2})일").matcher(message);
        if (matcher.find()) {
            int month = Integer.parseInt(matcher.group(1));
            int day = Integer.parseInt(matcher.group(2));
            return LocalDate.of(LocalDate.now().getYear(), month, day);
        }

        // 상대 날짜 처리
        if (message.contains("오늘")) {
            return LocalDate.now();
        } else if (message.contains("모레")) {
            return LocalDate.now().plusDays(2);
        } else if (message.contains("내일")) {
            return LocalDate.now().plusDays(1);
        } else if (message.contains("글피")) {
            return LocalDate.now().plusDays(3);
        } else if (message.contains("어제")) {
            return LocalDate.now().minusDays(1);
        } else if (message.contains("그제")) {
            return LocalDate.now().minusDays(2);
        }

        throw new IllegalArgumentException("날짜를 이해할 수 없습니다.");
    }
}