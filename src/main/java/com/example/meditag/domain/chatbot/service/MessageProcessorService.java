package com.example.meditag.domain.chatbot.service;

import com.example.meditag.domain.chatbot.entity.ChatSession;
import com.example.meditag.domain.chatbot.entity.Message;
import com.example.meditag.domain.chatbot.repository.ChatSessionRepository;
import com.example.meditag.domain.chatbot.repository.MessageRepository;
import com.example.meditag.domain.chatbot.service.message.DateMedicineInquiryService;
import com.example.meditag.domain.chatbot.service.message.DosageToggleService;
import com.example.meditag.domain.chatbot.service.message.GPTAnswerService;
import com.example.meditag.domain.chatbot.service.message.RecordingPlayService;
import com.example.meditag.domain.chatbot.service.message.register.MedicineRegisterService;
import com.example.meditag.global.error.exception.CustomException;
import com.example.meditag.global.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    /** 직전 턴의 ‘복용 토글’ 미완성 문장 저장 (사용자별) */
    private final Map<String, String> lastIncompleteDosageMessage = new ConcurrentHashMap<>();
    /** 직전 턴에서 양자택일(토글 vs 조회) 질문을 던졌는지 여부 (사용자별) */
    private final Map<String, Boolean> awaitingDisambiguation = new ConcurrentHashMap<>();

    public String processMessage(String username, Long chatSessionId, String message) {

        ChatSession session = chatSessionRepository.findById(chatSessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_SESSION_NOT_FOUND));

        // USER 메시지 저장
        messageRepository.save(Message.builder()
                .chatSession(session)
                .sender(Message.Sender.USER)
                .content(message)
                .build());

        try {
            // 0) 약 등록 플로우 진행 중이면 우선 처리
            boolean wasInRegisterFlow = medicineRegisterService.isInProgress(username);
            if (wasInRegisterFlow) {
                String out = medicineRegisterService.execute(username, message);
                // 등록 플로우가 종료되었으면 모든 분기/미완성 상태 초기화
                if (!medicineRegisterService.isInProgress(username)) {
                    resetEphemeralStates(username);
                }
                saveBot(session, out);
                return out;
            }

            // 1) 직전 턴이 양자택일 상태였다면 이번 메시지로 분기
            if (awaitingDisambiguation.getOrDefault(username, false)) {
                IntentChoice choice = pickFromDisambiguation(message);
                awaitingDisambiguation.remove(username);

                // ✅ 직전 사용자 발화와 결합 (예: "오늘 아침 탈모약 먹었어" + "기록해줘")
                String base = lastIncompleteDosageMessage.getOrDefault(username, "");
                String combined = base.isEmpty() ? message : (base + " " + message);

                String out;
                if (choice == IntentChoice.TOGGLE) {
                    out = dosageToggleService.execute(username, combined);
                    // 토글 성공/실패와 관계없이 임시 상태 정리
                    clearIncompleteDosage(username);
                } else {
                    // 조회의 경우 굳이 결합할 필요 없음
                    out = safeDateInquiry(username, message);
                    clearIncompleteDosage(username);
                }
                saveBot(session, out);
                return out;
            }

            // 2) 등록/토글 키워드 우선 처리
            if (medicineRegisterServiceTrigger(message)) {
                String out = medicineRegisterService.execute(username, message);
                if (!medicineRegisterService.isInProgress(username)) {
                    resetEphemeralStates(username);
                }
                saveBot(session, out);
                return out;
            }

            if (dosageToggleService.isApplicable(message)) {
                String out = dosageToggleService.execute(username, message);
                // ‘시간대만’ 후속 입력을 대비해 미완성 여부 갱신
                if (isIncompleteResponse(out)) {
                    lastIncompleteDosageMessage.put(username, message);
                } else {
                    clearIncompleteDosage(username);
                }
                saveBot(session, out);
                return out;
            }

            // 3) 후속 ‘시간대만’ 메시지라면 직전 미완성 문장과 결합
            if (isTimeOnlyMessage(message) && lastIncompleteDosageMessage.containsKey(username)) {
                String combined = lastIncompleteDosageMessage.get(username) + " " + message;
                String out = dosageToggleService.execute(username, combined);
                clearIncompleteDosage(username);
                saveBot(session, out);
                return out;
            }

            // 4) 조회 의도면 날짜 질의 처리
            if (isInquiryByKeywords(message)) {
                String out = safeDateInquiry(username, message);
                saveBot(session, out);
                return out;
            }

            // 5) 애매한 단문 → 양자택일 유도
            if (isAmbiguousBareUtterance(message) || triggersAlwaysDisambiguation(message)) {
                String ask = disambiguationPrompt();
                // ✅ 양자택일로 진입하는 시점에 직전 발화를 저장해 둔다.
                lastIncompleteDosageMessage.put(username, message);
                awaitingDisambiguation.put(username, true);
                saveBot(session, ask);
                return ask;
            }

            // 6) 기타 → GPT
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

    private void saveBot(ChatSession session, String content) {
        messageRepository.save(Message.builder()
                .chatSession(session)
                .sender(Message.Sender.BOT)
                .content(content)
                .build());
    }

    private void resetEphemeralStates(String username) {
        awaitingDisambiguation.remove(username);
        clearIncompleteDosage(username);
    }

    private void clearIncompleteDosage(String username) {
        lastIncompleteDosageMessage.remove(username);
    }

    // ===== 아래의 분기 판별 로직은 기존 프로젝트 규칙을 그대로 유지 =====

    private boolean medicineRegisterServiceTrigger(String msg) {
        // 기존 프로젝트의 등록 트리거와 동일하게 맞춰주세요.
        // (예: "약 등록", "약 추가", "등록할게" 등)
        String m = msg.replaceAll("\\s+", "");
        return m.matches(".*(약|알림).*등록(할게|할래|해줘|해|하고싶어|하고싶어|추가).*");
    }

    private boolean isInquiryByKeywords(String msg) {
        String m = msg.replaceAll("\\s+", "");
        if (m.matches(".*(알려줘|보여줘|확인|현황|리스트|조회|상태|몇개|남은|먹을약|먹어야).*")) return true;
        if (m.matches(".*(오늘|지금).*(약|복용).*(뭐|뭐야|있어|알려줘).*")) return true;
        return false;
    }

    private boolean isAmbiguousBareUtterance(String msg) {
        String m = msg.replaceAll("\\s+", "");
        return m.matches("^(약|복용|먹었어|먹었냐|먹을까|먹자|먹을래|약먹자|약먹을까)$");
    }

    private boolean triggersAlwaysDisambiguation(String msg) {
        String m = msg.replaceAll("\\s+", "");
        return m.matches(".*(먹었어|복용했어|복용완료).*"); // 완료/기록 vs 조회 중의 모호성
    }

    private boolean isTimeOnlyMessage(String msg) {
        String m = msg.replaceAll("\\s+", "");
        return m.matches(".*((오전|오후)?[0-2]?\\d시(\\d{1,2}분)?)|(아침|점심|저녁|자정|취침).*");
    }

    private boolean isIncompleteResponse(String serviceOutput) {
        return serviceOutput != null && serviceOutput.contains("어느 시간대");
    }

    private enum IntentChoice { TOGGLE, INQUIRY }

    private IntentChoice pickFromDisambiguation(String msg) {
        String m = msg == null ? "" : msg.replaceAll("\\s+", "");
        if (m.matches(".*(완료|기록|체크|변경|표시|했어|했어요|했지).*")) return IntentChoice.TOGGLE;
        return IntentChoice.INQUIRY;
    }

    private String disambiguationPrompt() {
        return "지금 ‘복용 완료로 기록’할까요, 아니면 ‘오늘 복용 현황을 알려드릴까요’? (예: 기록해줘 / 현황 알려줘)";
    }

    // ================== 헬퍼 ==================

    /** 날짜 질의 실행 시 어떤 예외가 나도 안전하게 사용자 메시지를 반환 */
    private String safeDateInquiry(String username, String message) {
        try {
            return dateMedicineInquiryService.execute(username, message);
        } catch (Exception ex) {
            log.warn("date inquiry failed, fallback to GPT. cause={}", ex.toString());
            // 여기서 GPT 호출도 실패할 수 있으니 다시 한 번 예외 안전 처리
            try {
                return gptAnswerService.execute(
                        username,
                        null,
                        "사용자가 복용 현황을 확인하려고 해. 오늘 기준 복용 현황을 먼저 보여주고, 필요하면 다른 날짜도 물어봐줘."
                );
            } catch (Exception gptEx) {
                log.error("GPT fallback also failed", gptEx);
                return "답변 생성 중 오류가 발생했어요. 다시 한번 말씀해주시겠어요?";
            }
        }
    }
}
