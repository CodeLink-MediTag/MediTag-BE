package com.example.meditag.domain.chatbot.service.message;

import com.example.meditag.domain.chatbot.entity.ChatSession;
import com.example.meditag.domain.chatbot.entity.Message;
import com.example.meditag.domain.chatbot.repository.ChatSessionRepository;
import com.example.meditag.domain.chatbot.repository.MessageRepository;
import com.example.meditag.domain.chatbot.service.OpenAiService;
import com.example.meditag.domain.chatbot.service.message.register.MedicineRegisterService;
import com.example.meditag.global.error.exception.CustomException;
import com.example.meditag.global.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProcessorService {

    private final ChatSessionRepository chatSessionRepository;
    private final MessageRepository messageRepository;

    private final MedicineRegisterService medicineRegisterService;
    private final RecordingPlayService recordingPlayService;
    private final DateMedicineInquiryService dateMedicineInquiryService;
    private final DosageToggleService dosageToggleService;
    private final GPTAnswerService gptAnswerService;
    private final OpenAiService openAiService;

    /** ‘복용 토글’ 미완성 문장(사용자별) */
    private final Map<String, String> lastIncompleteDosageMessage = new ConcurrentHashMap<>();
    /** 직전 턴 양자택일 상태 여부(사용자별) */
    private final Map<String, Boolean> awaitingDisambiguation = new ConcurrentHashMap<>();
    /** 직전 언급 약명/시간대(사용자별) */
    private final Map<String, String> lastMentionedMedicine = new ConcurrentHashMap<>();
    private final Map<String, String> lastMentionedTime = new ConcurrentHashMap<>();

    /** 약명으로 보면 안 되는 금지 토큰 */
    private static final Set<String> MEDICINE_FORBIDDEN_TOKENS = Set.of(
            "먹을", "먹을약", "먹어야", "먹어야할", "복용할", "남은", "아직안먹은",
            "무슨", "뭐야", "뭐임", "어떤", "모든", "약들", "약이", "약은"
    );

    public String processMessage(String username, Long chatSessionId, String message) {
        ChatSession session = chatSessionRepository.findById(chatSessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_SESSION_NOT_FOUND));

        saveUser(session, message);

        try {
            // 0) 약 등록 플로우가 진행 중이면 우선 처리
            if (medicineRegisterService.isInProgress(username)) {
                String out = medicineRegisterService.execute(username, message);
                if (!medicineRegisterService.isInProgress(username)) resetEphemeralStates(username);
                saveBot(session, out);
                return out;
            }

            // 0-1) 과거형 “먹었어/복용했어” → 반드시 양자택일 유도
            if (triggersAlwaysDisambiguation(message)) {
                awaitingDisambiguation.put(username, true);
                lastIncompleteDosageMessage.put(username, message);
                rememberContext(username, message);
                String q = disambiguationPrompt();
                saveBot(session, q);
                return q;
            }

            // 0-2) “오늘 먹을 약/남은 약/아직 안 먹은 약 …” → 무조건 ‘조회’ 라우팅
            if (isFutureOrRemainingIntakeQuery(message)) {
                String out = safeDateInquiry(username, message);
                rememberContext(username, message);
                saveBot(session, out);
                return out;
            }

            // 1) 직전 턴이 양자택일 상태였다면 이번 메시지로 분기
            if (awaitingDisambiguation.getOrDefault(username, false)) {
                IntentChoice choice = pickFromDisambiguation(message);
                awaitingDisambiguation.remove(username);

                String intentReady = completeWithMemory(username, message);
                String base = lastIncompleteDosageMessage.getOrDefault(username, "");
                String combined = base.isBlank() ? intentReady : (base + " " + intentReady);

                String out;
                if (choice == IntentChoice.TOGGLE) out = dosageToggleService.execute(username, combined);
                else out = safeDateInquiry(username, combined);

                clearIncompleteDosage(username);
                rememberContext(username, combined);
                saveBot(session, out);
                return out;
            }

            // 2) 등록/토글 우선 처리
            if (medicineRegisterServiceTrigger(message)) {
                String out = medicineRegisterService.execute(username, message);
                if (!medicineRegisterService.isInProgress(username)) resetEphemeralStates(username);
                saveBot(session, out);
                return out;
            }

            if (dosageToggleService.isApplicable(message)) {
                rememberContext(username, message);
                String out = dosageToggleService.execute(username, message);
                if (isIncompleteResponse(out)) lastIncompleteDosageMessage.put(username, message);
                else clearIncompleteDosage(username);
                saveBot(session, out);
                return out;
            }

            // 3) “아침약/아침/탈모약” 같은 단답 → 직전 컨텍스트 자동 결합 후 토글
            if (isTimeOnlyOrMedicineOnly(message)) {
                String completed = completeWithMemory(username, message);
                if (completed == null) {
                    String ask = "어느 시간대와 어떤 약인지 알려주세요. 예) '아침 탈모약'";
                    saveBot(session, ask);
                    return ask;
                }
                String out = dosageToggleService.execute(username, completed);
                clearIncompleteDosage(username);
                rememberContext(username, completed);
                saveBot(session, out);
                return out;
            }

            // 4) 일반 조회 의도
            if (isInquiryByKeywords(message)) {
                String out = safeDateInquiry(username, message);
                saveBot(session, out);
                return out;
            }

            // 5) 스몰토크 (안녕 등)
            if (isSmallTalk(message)) {
                String answer = smallTalkLocalReply(message);
                saveBot(session, answer);
                return answer;
            }

            // 6) 기타 → 앱 GPT
            String out = gptAnswerService.execute(username, chatSessionId, message);
            saveBot(session, out);
            return out;

        } catch (Exception e) {
            log.error("processMessage error", e);
            String fallback = "답변 생성 중 오류가 발생했어요. 다시 한번 말씀해주시겠어요?";
            saveBot(session, fallback);
            return fallback;
        }
    }

    /* ========= 컨텍스트 보관/보정 ========= */

    private void rememberContext(String username, String msg) {
        if (msg == null) return;
        String med = extractMedicine(msg);
        String time = extractTime(msg);
        if (med != null && !med.isBlank()) lastMentionedMedicine.put(username, med);
        if (time != null && !time.isBlank()) lastMentionedTime.put(username, time);
    }

    /** 단답(아침/아침약/탈모약 등)을 직전 맥락과 합쳐 완전 문장으로 보정 */
    private String completeWithMemory(String username, String msg) {
        String time = extractTime(msg);
        String med = extractMedicine(msg);
        if (time == null || time.isBlank()) time = lastMentionedTime.get(username);
        if (med  == null || med.isBlank())  med  = lastMentionedMedicine.get(username);
        if (time == null || med == null) return null;
        return "오늘 " + time + " " + med + " 먹었어";
    }

    private String extractMedicine(String msg) {
        if (msg == null) return null;
        String compact = msg.replaceAll("\\s+", "");

        // 금지 토큰 포함 시 약명으로 보지 않음
        for (String ban : MEDICINE_FORBIDDEN_TOKENS) {
            if (compact.contains(ban)) return null;
        }
        // “…약” 패턴 (탈모약/감기약/혈압약 등)
        Matcher m = Pattern.compile("([가-힣A-Za-z0-9]+)약").matcher(compact);
        if (m.find()) {
            String cand = m.group(1) + "약";
            if (!MEDICINE_FORBIDDEN_TOKENS.contains(cand)) return cand;
        }
        // ‘약’ 없는 상용명도 일부 지원
        Matcher m2 = Pattern.compile("(타이레놀|유산균|오메가3|비타민|멀티비타민|프로바이오틱스|아스피린|이부프로펜|아세트아미노펜)")
                .matcher(compact);
        if (m2.find()) return m2.group(1);
        return null;
    }

    private String extractTime(String msg) {
        if (msg == null) return null;
        String m = msg.replaceAll("\\s+", "");
        if (m.contains("아침약") || m.contains("아침")) return "아침";
        if (m.contains("점심약") || m.contains("점심")) return "점심";
        if (m.contains("저녁약") || m.contains("저녁")) return "저녁";
        return null;
    }

    private boolean isTimeOnlyOrMedicineOnly(String msg) {
        String t = extractTime(msg);
        String med = extractMedicine(msg);
        return (t != null && med == null) || (t == null && med != null) || msg.replaceAll("\\s+","").matches("(아침약|점심약|저녁약)");
    }

    /* ========= 저장 공통 ========= */

    private void resetEphemeralStates(String username) {
        awaitingDisambiguation.remove(username);
        lastIncompleteDosageMessage.remove(username);
    }
    private void clearIncompleteDosage(String username) {
        lastIncompleteDosageMessage.remove(username);
    }
    private void saveUser(ChatSession session, String content) {
        try {
            messageRepository.save(Message.builder()
                    .chatSession(session)
                    .sender(Message.Sender.USER)
                    .content(content)
                    .build());
        } catch (Exception e) { log.warn("user message save failed: {}", e.toString()); }
    }
    private void saveBot(ChatSession session, String content) {
        try {
            messageRepository.save(Message.builder()
                    .chatSession(session)
                    .sender(Message.Sender.BOT)
                    .content(content)
                    .build());
        } catch (Exception e) { log.warn("bot message save failed: {}", e.toString()); }
    }

    /* ========= 서비스 호출 래핑 ========= */

    private String safeDateInquiry(String username, String message) {
        try {
            return dateMedicineInquiryService.execute(username, message);
        } catch (Exception ex) {
            log.warn("date inquiry failed, fallback to GPT. cause={}", ex.toString());
            try {
                return gptAnswerService.execute(
                        username, null,
                        "사용자가 복용 현황/오늘 먹을 약을 확인하려고 해. 오늘 기준 남은 약을 먼저 보여주고, 필요하면 다른 시간대도 물어봐줘."
                );
            } catch (Exception gptEx) {
                log.error("GPT fallback also failed", gptEx);
                return "답변 생성 중 오류가 발생했어요. 다시 한번 말씀해주시겠어요?";
            }
        }
    }

    /* ========= 분류/규칙 ========= */

    private boolean medicineRegisterServiceTrigger(String msg) {
        String m = msg.replaceAll("\\s+", "");
        return m.matches(".*(약|알림).*등록(할게|할래|해줘|해|하고싶어|추가).*");
    }

    /** 일반 조회 키워드 (기존) */
    private boolean isInquiryByKeywords(String msg) {
        String m = msg.replaceAll("\\s+", "");
        if (m.matches(".*(알려줘|보여줘|확인|현황|리스트|조회|상태|몇개|남은|먹을약|먹어야).*")) return true;
        if (m.matches(".*(오늘|지금).*(약|복용).*(뭐|뭐야|있어|알려줘).*")) return true;
        return false;
    }

    /** ★ ‘오늘 먹을/복용할/남은/미복용’ 류 → 무조건 조회 */
    private boolean isFutureOrRemainingIntakeQuery(String msg) {
        String m = msg.replaceAll("\\s+", "");
        return m.matches(".*((오늘|지금)?(먹을|복용할)약(뭐|뭐야|알려줘|있어)?).*")
                || m.matches(".*(먹어야(할)?약|남은약|아직안먹은약|미복용).*");
    }

    /** 과거형/확정형만 디스앰빅(미래형 ‘먹을’ 제외) */
    private boolean triggersAlwaysDisambiguation(String msg) {
        String m = msg.replaceAll("\\s+", "");
        return m.matches(".*(먹었어|먹음|먹었다|복용했어|복용완료).*");
    }

    private boolean isIncompleteResponse(String serviceOutput) {
        return serviceOutput != null && serviceOutput.contains("어느 시간대");
    }

    private enum IntentChoice { TOGGLE, INQUIRY }
    private IntentChoice pickFromDisambiguation(String msg) {
        String m = msg == null ? "" : msg.replaceAll("\\s+", "");
        if (m.matches(".*(완료|기록|체크|변경|표시|했어|했어요|했지|기록해줘|체크해줘).*")) return IntentChoice.TOGGLE;
        return IntentChoice.INQUIRY;
    }

    private String disambiguationPrompt() {
        return "지금 ‘복용 완료로 기록’할까요, 아니면 ‘오늘 복용 현황을 알려드릴까요’? (예: 기록해줘 / 현황 알려줘)";
    }

    /* ========= 스몰토크 ========= */

    private boolean isSmallTalk(String msg) {
        if (msg == null) return false;
        String m = msg.replaceAll("\\s+","").toLowerCase();
        return m.matches(".*(안녕|안뇽|하이|헬로|반가워|굿모닝|굿이브닝|잘지냈|고마워|감사|ㅎ+|ㅋㅋ+|ㅎㅎ+|반갑).*");
    }

    private String smallTalkLocalReply(String msg) {
        String m = msg == null ? "" : msg.replaceAll("\\s+","").toLowerCase();
        if (m.contains("안녕") || m.contains("하이") || m.contains("헬로") || m.contains("반가워")) {
            return "안녕하세요! 😊 무엇을 도와드릴까요? (예: ‘오늘 먹을 약 뭐야’, ‘현황 알려줘’, ‘오늘 아침 약 먹었어’)";
        }
        if (m.contains("고마워") || m.contains("감사")) {
            return "도움이 되어 기뻐요! 필요한 게 있으면 언제든 말씀해 주세요.";
        }
        return "네, 여기 있어요 👋 무엇을 도와드릴까요?";
    }
}
