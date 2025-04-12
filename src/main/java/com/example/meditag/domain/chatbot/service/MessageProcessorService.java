package com.example.meditag.domain.chatbot.service;

import com.example.meditag.domain.chatbot.service.message.*;
import com.example.meditag.domain.chatbot.service.message.register.MedicineRegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class MessageProcessorService {

    private final DosageToggleService dosageToggleService;
    private final MedicineRegisterService medicineRegisterService;
    private final RecordingPlayService recordingPlayService;
    private final DateMedicineInquiryService dateMedicineInquiryService;
    private final GPTAnswerService gptAnswerService;

    // 사용자의 마지막 미완료 메시지 저장 (예: 약 이름이나 시간대 미포함)
    private final Map<String, String> lastIncompleteDosageMessage = new ConcurrentHashMap<>();

    public String processMessage(String username, Long chatSessionId, String message) {

        // 우선 약 등록 세션 중일 경우 처리
        if (medicineRegisterService.isInProgress(username)) {
            return medicineRegisterService.execute(username, message);
        }

        // 약 복용 여부 변경 처리
        if (dosageToggleService.isApplicable(message)) {
            String result = dosageToggleService.execute(username, message);
            if (isIncompleteResponse(result)) {
                lastIncompleteDosageMessage.put(username, message);
            } else {
                lastIncompleteDosageMessage.remove(username);
            }
            return result;
        }

        // 시간대만으로 메시지가 왔을 때 이전 메시지와 결합 처리
        if (isTimeOnlyMessage(message) && lastIncompleteDosageMessage.containsKey(username)) {
            String priorMessage = lastIncompleteDosageMessage.get(username);
            String combinedMessage = priorMessage + " " + message;
            String result = dosageToggleService.execute(username, combinedMessage);
            lastIncompleteDosageMessage.remove(username);
            return result;
        }

        // 약 알림 등록 시작
        if (medicineRegisterService.isApplicable(message)) {
            return medicineRegisterService.execute(username, message);
        }

        // 주의사항 녹음 재생
        if (recordingPlayService.isApplicable(message)) {
            return recordingPlayService.execute(username, message);
        }

        // 특정 날짜의 약 알림 조회
        if (dateMedicineInquiryService.isApplicable(message)) {
            return dateMedicineInquiryService.execute(username, message);
        }

        // 기타 메시지는 GPT로 처리
        return gptAnswerService.execute(username, chatSessionId, message);
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