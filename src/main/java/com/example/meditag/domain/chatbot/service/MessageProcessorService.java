package com.example.meditag.domain.chatbot.service;

import com.example.meditag.domain.chatbot.service.message.*;
import com.example.meditag.domain.chatbot.service.message.register.MedicineRegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageProcessorService {

    private final DosageToggleService dosageToggleService;
    private final MedicineRegisterService medicineRegisterService;
    private final RecordingPlayService recordingPlayService;
    private final DateMedicineInquiryService dateMedicineInquiryService;
    private final GPTAnswerService gptAnswerService;

    public String processMessage(String username, Long chatSessionId, String message) {

        // 현재 약 등록 세션이 진행 중인 경우 → 무조건 약 등록 흐름 유지
        if (medicineRegisterService.isInProgress(username)) {
            return medicineRegisterService.execute(username, message);
        }

        // 복용 여부 변경
        if (dosageToggleService.isApplicable(message)) {
            return dosageToggleService.execute(username, message);
        }

        // 약 알림 등록 시작
        if (medicineRegisterService.isApplicable(message)) {
            return medicineRegisterService.execute(username, message);
        }

        // 주의사항 녹음 재생
        if (recordingPlayService.isApplicable(message)) {
            return recordingPlayService.execute(username, message);
        }

        // 약 알림 조회
        if (dateMedicineInquiryService.isApplicable(message)) {
            return dateMedicineInquiryService.execute(username, message);
        }

        // 그 외 GPT 처리
        return gptAnswerService.execute(username, chatSessionId, message);
    }
}


