package com.example.meditag.domain.chatbot.service.message;

import com.example.meditag.domain.alarm.entity.Alarm;
import com.example.meditag.domain.alarm.repository.AlarmRepository;
import com.example.meditag.domain.chatbot.service.OpenAiService;
import com.example.meditag.domain.member.entity.Member;
import com.example.meditag.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GPTAnswerService {

    private final OpenAiService openAiService;
    private final AlarmRepository alarmRepository;
    private final MemberRepository memberRepository; // ✅ 이름 조회를 위해 주입

    public String execute(String username, Long chatSessionId, String userMessage) {
        try {
            String msg = (userMessage == null ? "" : userMessage.trim());

            // ✅ 0) "내 이름은 뭐야" 유형 선처리: 이메일 대신 회원 이름/닉네임을 반환
            if (isAskMyName(msg)) {
                String displayName = resolveDisplayName(username);
                return String.format("등록된 이름으로는 '%s'님이에요 🙂", displayName);
            }

            // 1) 사용자 컨텍스트와 함께 GPT에게 질문
            String aiResponse = askGptWithContext(username, msg);

            // 2) GPT 응답이 있으면 그대로 반환
            if (aiResponse != null && !aiResponse.isBlank()) {
                return aiResponse;
            }

            // 3) GPT 호출 실패 시 기본 메시지
            return "죄송해요, 잠시 문제가 생겼어요. 다시 말씀해 주시겠어요?";

        } catch (Exception e) {
            log.error("GPTAnswerService error: {}", e.getMessage());
            return "대답을 준비하다가 문제가 생겼어요. 다시 말씀해 주시겠어요?";
        }
    }

    /** "내 이름"을 묻는 다양한 표현을 감지 */
    private boolean isAskMyName(String message) {
        if (message == null || message.isBlank()) return false;
        String c = message.replaceAll("\\s+", "").toLowerCase();
        // 한국어/변형: 내이름, 내이름은뭐야, 내이름알려줘, 나는누구, 내가누구
        if (c.matches(".*(내이름|내이름은|내이름뭐야|내이름이뭐야|내이름알려줘|나는누구|내가누구).*")) return true;
        // 영문: what's my name / what is my name
        return c.contains("whatsmyname") || c.contains("whatismyname");
    }

    /**
     * 사용자 표시 이름 결정 로직
     * 1) Member.name → 2) Member.nickname → 3) 이메일 로컬파트( @ 앞 ) → 4) "사용자"
     */
    private String resolveDisplayName(String username) {
        try {
            Optional<Member> opt = memberRepository.findByUsername(username);
            if (opt.isPresent()) {
                Member m = opt.get();
                // name 필드가 있으면 우선 사용
                try {
                    String name = m.getName();
                    if (name != null && !name.isBlank()) return name.trim();
                } catch (Exception ignore) {}
            }
        } catch (Exception e) {
            log.warn("resolveDisplayName failed: {}", e.toString());
        }
        // 이메일에서 @ 앞 부분을 예비 표시명으로 사용
        if (username != null && username.contains("@")) {
            String local = username.substring(0, username.indexOf('@'));
            return local.isBlank() ? "사용자" : local;
        }
        return "사용자";
    }

    /**
     * 사용자의 약물 정보와 복용 기록을 포함한 컨텍스트를 만들어서 GPT에 질문
     */
    private String askGptWithContext(String username, String userQuestion) {
        try {
            // 1. 사용자의 오늘 약물 정보 수집
            LocalDate today = LocalDate.now();
            List<Alarm> todayAlarms = alarmRepository.findByUsernameAndDateTimeRange(
                    username, today.atStartOfDay(), today.atTime(23, 59, 59));

            // 2. 사용자 컨텍스트 구성
            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append("=== 사용자 정보 ===\n");
            contextBuilder.append("사용자명: ").append(resolveDisplayName(username)).append("\n"); // ✅ 이름 사용
            contextBuilder.append("계정ID: ").append(username).append("\n"); // 내부 식별용(필요 없다면 제거)
            contextBuilder.append("날짜: ").append(today.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"))).append("\n\n");

            if (todayAlarms != null && !todayAlarms.isEmpty()) {
                contextBuilder.append("=== 오늘 복용 약물 정보 ===\n");
                for (Alarm alarm : todayAlarms) {
                    String medicineName = safeName(alarm);
                    String time = String.format("%02d:%02d", alarm.getAlarmTime().getHour(), alarm.getAlarmTime().getMinute());
                    String status = alarm.isTaking() ? "복용 완료" : "미복용";
                    contextBuilder.append(String.format("- %s: %s (%s)\n", time, medicineName, status));
                }
                contextBuilder.append("\n");
            } else {
                contextBuilder.append("=== 오늘 복용 약물 정보 ===\n");
                contextBuilder.append("오늘 등록된 약물 일정이 없습니다.\n\n");
            }

            // 3. 시스템 프롬프트 구성
            String systemPrompt = """
                당신은 시각장애인을 위한 복약 도우미 앱 '메디태그'의 친절한 AI 비서입니다.
                
                역할과 성격:
                - 친근하고 따뜻한 친구처럼 대화합니다
                - 사용자의 모든 질문에 자연스럽게 답변합니다
                - 복약 관련이든 일상 대화든 자유롭게 대화합니다
                - 이모지를 적절히 사용해서 친근하게 답변합니다
                
                답변 가이드:
                1. 약 관련 질문 (예: "머리 아픈데 무슨 약 먹을까?", "타이레놀이 뭐야?")
                   - 일반적인 약 정보와 조언을 제공합니다
                   - 심각한 증상이면 병원 방문을 권유합니다
                   - 사용자가 현재 복용 중인 약과의 상호작용을 고려합니다
                
                2. 복약 일정 관련 질문 (예: "오늘 약 먹었어?", "남은 약 뭐야?")
                   - 위에 제공된 사용자 정보를 활용해서 답변합니다
                   - 구체적이고 정확하게 알려줍니다
                
                3. 일상 대화 (예: "오늘 날씨 어때?", "기분 좋아")
                   - 자연스럽게 공감하고 대화합니다
                   - 필요하면 복약도 챙기라고 자연스럽게 언급합니다
                
                4. 건강 상담 (예: "머리가 아파", "소화가 안돼")
                   - 공감하고 일반적인 대처법을 알려줍니다
                   - 증상이 심하면 전문의 상담을 권유합니다
                
                주의사항:
                - 명시적인 '등록/삭제/변경' 요청이 없으면 DB 변경을 제안하지 마세요
                - 전문적인 의학적 진단은 하지 마세요
                - 답변은 2-4문장 정도로 간결하게 작성하세요
                - "복약 외 질문도 가능해요", "지금은~" 같은 메타적 멘트는 절대 하지 마세요
                - 질문에 바로 답하세요
                """;

            // 4. 전체 프롬프트 구성
            String fullPrompt = contextBuilder.toString() +
                    "=== 사용자 질문 ===\n" + userQuestion;

            log.info("GPT 호출 - 사용자: {}, 질문: {}", username, userQuestion);

            // 5. OpenAI API 호출
            return openAiService.sendMessageWithSystem(systemPrompt, fullPrompt);

        } catch (Exception e) {
            log.warn("GPT 컨텍스트 호출 실패: {}", e.getMessage());
            return null;
        }
    }

    private String safeName(Alarm a) {
        if (a.getCalendar() != null && a.getCalendar().getMedicine() != null) {
            return a.getCalendar().getMedicine().getName();
        }
        return "등록명 없음";
    }
}
