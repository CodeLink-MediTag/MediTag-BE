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
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class MessageProcessorService {

    private final MessageRepository messageRepository;
    private final ChatSessionRepository chatSessionRepository;

    private final DosageToggleService dosageToggleService;
    private final MedicineRegisterService medicineRegisterService;
    private final RecordingPlayService recordingPlayService;
    private final DateMedicineInquiryService dateMedicineInquiryService;
    private final GPTAnswerService gptAnswerService;

    // 사용자가 직전에 '토글' 흐름을 불완전하게 끝낸 메시지 저장
    private final Map<String, String> lastIncompleteDosageMessage = new ConcurrentHashMap<>();

    // 직전 턴에서 양자택일(토글 vs 조회) 질문을 던졌는지 여부
    private final Map<String, Boolean> awaitingDisambiguation = new ConcurrentHashMap<>();

    public String processMessage(String username, Long chatSessionId, String message) {

        ChatSession session = chatSessionRepository.findById(chatSessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_SESSION_NOT_FOUND));

        // 사용자 발화 저장
        messageRepository.save(Message.builder()
                .chatSession(session)
                .sender(Message.Sender.USER)
                .content(message)
                .build());

        try {
            // 0) 약 등록 플로우가 진행 중이면 우선 처리
            if (medicineRegisterService.isInProgress(username)) {
                String result = medicineRegisterService.execute(username, message);
                saveBotMessage(session, result);
                return result;
            }

            // 1) 직전 턴에 '양자택일 질문'을 던진 상태라면, 이번 메시지로 분기 결정
            if (awaitingDisambiguation.getOrDefault(username, false)) {
                IntentChoice choice = pickFromDisambiguation(message);
                if (choice == IntentChoice.TOGGLE) {
                    String result = dosageToggleService.execute(username, message);
                    awaitingDisambiguation.remove(username);
                    saveBotMessage(session, result);
                    return result;
                } else if (choice == IntentChoice.INQUIRE) {
                    try {
                        String result = dateMedicineInquiryService.execute(username, message);
                        awaitingDisambiguation.remove(username);
                        saveBotMessage(session, result);
                        return result;
                    } catch (Exception e) {
                        String fallback = gptAnswerService.execute(
                                username,
                                chatSessionId,
                                "사용자가 복용 현황을 보려는 것 같지만 날짜를 확정하지 못했어. 오늘 복용 현황을 먼저 보여주고, 필요하면 다른 날짜도 물어봐줘."
                        );
                        saveBotMessage(session, fallback);
                        return fallback;
                    }
                } else {
                    // 여전히 모호하면 한 번 더 유도
                    String ask = disambiguationPrompt();
                    saveBotMessage(session, ask);
                    return ask;
                }
            }

            // 2) **항상-분기 유도**: “약 … 먹었어/복용했어/복용완료 …” 류면 무조건 양자택일 질문
            if (triggersAlwaysDisambiguation(message)) {
                String ask = disambiguationPrompt();
                awaitingDisambiguation.put(username, true);
                saveBotMessage(session, ask);
                return ask;
            }

            // 3) 최종 어절/표현 단서로 '조회(질문)' 의도 우선 감지
            if (isInquiryByKeywords(message)) {
                try {
                    String result = dateMedicineInquiryService.execute(username, message);
                    saveBotMessage(session, result);
                    return result;
                } catch (Exception e) {
                    String fallback = gptAnswerService.execute(
                            username,
                            chatSessionId,
                            "사용자가 복용 현황을 확인하려고 해. 오늘 기준 복용 현황을 먼저 보여주고, 필요하면 다른 날짜도 물어봐줘."
                    );
                    saveBotMessage(session, fallback);
                    return fallback;
                }
            }

            // 4) 최종 어절/표현 단서로 '토글(완료 기록)' 의도 감지
            if (isToggleByKeywords(message)) {
                String result = dosageToggleService.execute(username, message);
                if (isIncompleteResponse(result)) {
                    lastIncompleteDosageMessage.put(username, message);
                } else {
                    lastIncompleteDosageMessage.remove(username);
                }
                saveBotMessage(session, result);
                return result;
            }

            // 5) (레거시) 애매한 단문이면 → 양자택일 질문
            if (isAmbiguousBareUtterance(message)) {
                String ask = disambiguationPrompt();
                awaitingDisambiguation.put(username, true);
                saveBotMessage(session, ask);
                return ask;
            }

            // === 기존 로직 ===

            // (A) 토글 서비스가 적용 가능하면 처리
            if (dosageToggleService.isApplicable(message)) {
                String result = dosageToggleService.execute(username, message);
                if (isIncompleteResponse(result)) {
                    lastIncompleteDosageMessage.put(username, message);
                } else {
                    lastIncompleteDosageMessage.remove(username);
                }
                saveBotMessage(session, result);
                return result;
            }

            // (B) 시간대만 있는 후속 메시지 결합 처리
            if (isTimeOnlyMessage(message) && lastIncompleteDosageMessage.containsKey(username)) {
                String priorMessage = lastIncompleteDosageMessage.get(username);
                String combinedMessage = priorMessage + " " + message;
                String result = dosageToggleService.execute(username, combinedMessage);
                lastIncompleteDosageMessage.remove(username);
                saveBotMessage(session, result);
                return result;
            }

            // (C) 약 등록 시작 요청
            if (medicineRegisterService.isApplicable(message)) {
                try {
                    String result = medicineRegisterService.execute(username, message);
                    saveBotMessage(session, result);
                    return result;
                } catch (Exception e) {
                    String fallback = gptAnswerService.execute(
                            username,
                            chatSessionId,
                            "사용자가 약 등록을 원하지만 이해하지 못했어. 다음 질문을 자연스럽게 해줘."
                    );
                    saveBotMessage(session, fallback);
                    return fallback;
                }
            }

            // (D) 녹음 재생
            if (recordingPlayService.isApplicable(message)) {
                String result = recordingPlayService.execute(username, message);
                saveBotMessage(session, result);
                return result;
            }

            // (E) 날짜 기반 조회
            if (dateMedicineInquiryService.isApplicable(message)) {
                try {
                    String result = dateMedicineInquiryService.execute(username, message);
                    saveBotMessage(session, result);
                    return result;
                } catch (Exception e) {
                    String fallback = gptAnswerService.execute(
                            username,
                            chatSessionId,
                            "사용자가 날짜를 말했는데 이해하지 못했어. 무슨 날짜를 원하는지 다시 물어봐줘."
                    );
                    saveBotMessage(session, fallback);
                    return fallback;
                }
            }

            // (F) 일반 GPT 응답
            String result = gptAnswerService.execute(username, chatSessionId, message);
            saveBotMessage(session, result);
            return result;

        } catch (Exception e) {
            String fallback = gptAnswerService.execute(
                    username,
                    chatSessionId,
                    "사용자의 말을 이해하지 못했거나 오류가 발생했어. 자연스럽게 다시 물어봐줘."
            );
            saveBotMessage(session, fallback);
            return fallback;
        }
    }

    // === 유틸리티들 ===

    private void saveBotMessage(ChatSession session, String content) {
        messageRepository.save(Message.builder()
                .chatSession(session)
                .sender(Message.Sender.BOT)
                .content(content)
                .build());
    }

    /**
     * “약 … 먹었어/복용했어/복용완료 …” 류면 시간/약 언급 여부와 무관하게 항상 양자택일 유도
     */
    private boolean triggersAlwaysDisambiguation(String msg) {
        String compact = msg == null ? "" : msg.replaceAll("\\s+", "");
        return compact.matches(".*약.*(먹었어|먹음|먹었습니다|복용했어|복용함|복용했습니다|복용완료|먹었어요|복용했어요).*");
    }

    /**
     * 애매한 단문(레거시 유지): 약복용했어/오늘약먹었어 등
     */
    private boolean isAmbiguousBareUtterance(String msg) {
        String m = msg.trim().replaceAll("\\s+", "");
        if (m.equals("약복용했어")
                || m.equals("복용했어")
                || m.equals("오늘약먹었어")
                || m.equals("오늘약복용했어")
                || m.equals("오늘약다먹었어")
                || m.equals("오늘약은먹었어")) {
            return true;
        }
        String compact = msg.replaceAll("\\s+", "");
        return compact.matches(".*오늘.*약.*(이미|전부|다|은)?먹었어.*");
    }

    // 양자택일 질문 문구
    private String disambiguationPrompt() {
        return "지금 ‘복용 완료로 기록’할까요, 아니면 ‘오늘 복용 현황을 알려드릴까요’?";
    }

    // 사용자 답에서 의도 선택
    private IntentChoice pickFromDisambiguation(String userMsg) {
        String m = userMsg.replaceAll("\\s+", "");
        // 완료/기록류
        if (m.matches(".*(완료|기록|처리|체크|변경|표시).*")) return IntentChoice.TOGGLE;
        // 조회/현황류
        if (m.matches(".*(현황|상태|알려줘|보여줘|조회|확인).*")) return IntentChoice.INQUIRE;
        return IntentChoice.UNKNOWN;
    }

    private enum IntentChoice { TOGGLE, INQUIRE, UNKNOWN }

    /**
     * "아침/점심/저녁약"만 있는지(약 이름 없이) 판단
     */
    private boolean isTimeOnlyMessage(String message) {
        return message.matches(".*(아침|점심|저녁)약.*")
                && !message.matches(".*(타이레놀|탈모약|감기약|혈압약|위장약).*");
    }

    /**
     * 토글 응답이 불완전한지(추가 정보 요청/알람 없음 등)
     */
    private boolean isIncompleteResponse(String response) {
        return response.contains("어느 시간대의 약인지")
                || response.contains("복용 알람이 없어요");
    }

    // 조회(질문) 의도 키워드
    private boolean isInquiryByKeywords(String msg) {
        String m = msg.replaceAll("\\s+", "");
        if (m.matches(".*(알려줘|보여줘|어때|어찌|인지|여부|확인|현황|리스트|조회|상태|몇개|남은).*")) return true;
        if (m.matches(".*(오늘|지금).*(어때|어떻게).*")) return true;
        return false;
    }

    // 토글(완료/기록) 의도 키워드
    private boolean isToggleByKeywords(String msg) {
        String m = msg.replaceAll("\\s+", "");
        // 완료/기록/처리/체크/변경/표시/먹었어/복용했어 등
        if (m.matches(".*(완료|기록|처리|체크|변경|표시|먹었어|복용했어|먹었습니다|복용했습니다|복용완료).*")) return true;
        // 시간대 단서 + 행위
        if (m.matches(".*(아침|점심|저녁|자정|취침|[0-2]?\\d시).*(완료|기록|처리|체크|변경|표시).*")) return true;
        return false;
    }
}
