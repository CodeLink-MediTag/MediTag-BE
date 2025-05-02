package com.example.meditag.domain.chatbot.service;

import com.example.meditag.domain.chatbot.entity.ChatSession;
import com.example.meditag.domain.chatbot.entity.Message;
import com.example.meditag.domain.chatbot.repository.ChatSessionRepository;
import com.example.meditag.domain.chatbot.repository.MessageRepository;
import com.example.meditag.domain.chatbot.service.message.*;
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

    private final Map<String, String> lastIncompleteDosageMessage = new ConcurrentHashMap<>();

    public String processMessage(String username, Long chatSessionId, String message) {

        ChatSession session = chatSessionRepository.findById(chatSessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_SESSION_NOT_FOUND));

        messageRepository.save(Message.builder()
                .chatSession(session)
                .sender(Message.Sender.USER)
                .content(message)
                .build());

        try {
            if (medicineRegisterService.isInProgress(username)) {
                String result = medicineRegisterService.execute(username, message);
                saveBotMessage(session, result);
                return result;
            }

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

            if (isTimeOnlyMessage(message) && lastIncompleteDosageMessage.containsKey(username)) {
                String priorMessage = lastIncompleteDosageMessage.get(username);
                String combinedMessage = priorMessage + " " + message;
                String result = dosageToggleService.execute(username, combinedMessage);
                lastIncompleteDosageMessage.remove(username);
                saveBotMessage(session, result);
                return result;
            }

            if (medicineRegisterService.isApplicable(message)) {
                try {
                    String result = medicineRegisterService.execute(username, message);
                    saveBotMessage(session, result);
                    return result;
                } catch (Exception e) {
                    String fallback = gptAnswerService.execute(username, chatSessionId,
                            "사용자가 약 등록을 원하지만 이해하지 못했어. 다음 질문을 자연스럽게 해줘.");
                    saveBotMessage(session, fallback);
                    return fallback;
                }
            }

            if (recordingPlayService.isApplicable(message)) {
                String result = recordingPlayService.execute(username, message);
                saveBotMessage(session, result);
                return result;
            }

            if (dateMedicineInquiryService.isApplicable(message)) {
                try {
                    String result = dateMedicineInquiryService.execute(username, message);
                    saveBotMessage(session, result);
                    return result;
                } catch (Exception e) {
                    String fallback = gptAnswerService.execute(username, chatSessionId,
                            "사용자가 날짜를 말했는데 이해하지 못했어. 무슨 날짜를 원하는지 다시 물어봐줘.");
                    saveBotMessage(session, fallback);
                    return fallback;
                }
            }

            String result = gptAnswerService.execute(username, chatSessionId, message);
            saveBotMessage(session, result);
            return result;

        } catch (Exception e) {
            String fallback = gptAnswerService.execute(username, chatSessionId,
                    "사용자의 말을 이해하지 못했거나 오류가 발생했어. 자연스럽게 다시 물어봐줘.");
            saveBotMessage(session, fallback);
            return fallback;
        }
    }

    private void saveBotMessage(ChatSession session, String content) {
        messageRepository.save(Message.builder()
                .chatSession(session)
                .sender(Message.Sender.BOT)
                .content(content)
                .build());
    }

    private boolean isTimeOnlyMessage(String message) {
        return message.matches(".*(아침|점심|저녁)약.*")
                && !message.matches(".*(타이레놀|탈모약|감기약|혈압약|위장약).*");
    }

    private boolean isIncompleteResponse(String response) {
        return response.contains("어느 시간대의 약인지")
                || response.contains("복용 알람이 없어요");
    }
}
