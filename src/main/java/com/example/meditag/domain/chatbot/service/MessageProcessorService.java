package com.example.meditag.domain.chatbot.service;

import com.example.meditag.domain.chatbot.service.message.DateMedicineInquiryService;
import com.example.meditag.domain.chatbot.service.message.DosageToggleService;
import com.example.meditag.domain.chatbot.service.message.GPTAnswerService;
import com.example.meditag.domain.chatbot.service.message.IntakeStatusInquiryService;
import com.example.meditag.domain.chatbot.service.message.RecordingPlayService;
import com.example.meditag.domain.chatbot.service.message.register.MedicineRegisterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageProcessorService {

    private final DateMedicineInquiryService dateMedicineInquiryService;
    private final DosageToggleService dosageToggleService;
    private final IntakeStatusInquiryService intakeStatusInquiryService;
    private final RecordingPlayService recordingPlayService;
    private final MedicineRegisterService medicineRegisterService;
    private final GPTAnswerService gptAnswerService;

    public String process(String username, Long chatSessionId, String text) {
        final String msg = text == null ? "" : text.trim();

        try {
            // 0) 빈 입력/명확한 인사만 웰컴
            if (msg.isBlank() || isHello(msg)) return helloReply();

            // 1) 앱 소개
            if (isAppIntro(msg)) return appIntroReply();

            // 2) 약 등록
            if (medicineRegisterService != null &&
                    (medicineRegisterService.isInProgress(username) || isRegisterTrigger(msg))) {
                return medicineRegisterService.execute(username, msg);
            }

            // 3) 현황/질문형
            if (intakeStatusInquiryService != null && intakeStatusInquiryService.isApplicable(msg)) {
                return intakeStatusInquiryService.execute(username, msg);
            }

            // 4) 토글
            if (dosageToggleService != null && dosageToggleService.isApplicable(msg)) {
                return dosageToggleService.execute(username, msg);
            }

            // 5) 날짜/시간대 조회
            if (isDateInquiry(msg)) {
                return dateMedicineInquiryService.execute(username, msg);
            }

            // 6) 녹음 재생
            if (recordingPlayService != null && recordingPlayService.isApplicable(msg)) {
                return recordingPlayService.execute(username, msg);
            }

            // 7) 기타 → GPT 답변
            return gptAnswerService.execute(username, chatSessionId, msg);

        } catch (Exception e) {
            log.error("Message processing failed: {}", e.getMessage(), e);
            return "처리 중 문제가 발생했어요. 잠시 후 다시 시도해 주세요.";
        }
    }

    private boolean isHello(String s) {
        String c = compact(s);
        return c.matches("(?i)^(안녕|안뇽|하이|hi|hello|헬로)$");
    }

    private boolean isAppIntro(String s) {
        String c = compact(s);
        return c.contains("앱소개") || c.contains("앱설명") || c.contains("어플소개")
                || c.contains("서비스소개") || c.contains("무슨앱") || c.contains("무슨기능");
    }

    private boolean isRegisterTrigger(String s) {
        String c = compact(s);
        return c.contains("약등록") || c.contains("등록") || c.contains("추가");
    }

    private boolean isDateInquiry(String s) {
        String c = compact(s);
        if (c.isEmpty()) return false;

        // 오늘/내일/모레/특정 날짜 + 먹을/먹어야할/복용할 + 약
        if (c.matches(".*((오늘|내일|모레|\\d{1,2}월\\d{1,2}일)?(먹을|먹어야할|복용할)약(알려줘|머야|뭐야|뭐)).*")) {
            return true;
        }

        // 날짜 없이도 오늘/내일/모레 패턴 + 약 + 뭐야/알려줘/목록/리스트
        if ((c.contains("오늘") || c.contains("내일") || c.contains("모레") || c.matches(".*\\d{1,2}월\\d{1,2}일.*"))
                && c.contains("약")
                && (c.contains("뭐야") || c.contains("머야") || c.contains("알려줘") || c.contains("목록") || c.contains("리스트"))) {
            if (!(c.contains("먹은") && !c.contains("먹을")) && !c.contains("미복용") && !c.contains("남은")
                    && !(c.contains("아직") && c.contains("안") && c.contains("먹"))) {
                return true;
            }
        }

        // 시간대 + 약 + 뭐야/알려줘/리스트
        if ((c.contains("아침") || c.contains("점심") || c.contains("저녁"))
                && (c.contains("약뭐야") || c.contains("약뭐있어") || c.contains("뭐먹을")
                || c.contains("먹을약") || c.contains("알려줘") || c.contains("목록") || c.contains("리스트"))) {
            if (!(c.contains("먹은") && !c.contains("먹을")) && !c.contains("미복용") && !c.contains("남은")
                    && !(c.contains("아직") && c.contains("안") && c.contains("먹"))) {
                return true;
            }
        }

        // 기본값
        return false;
    }

    /** 공백 제거 + 소문자 */
    private String compact(String s) {
        return s == null ? "" : s.replaceAll("\\s+", "").toLowerCase();
    }

    private String helloReply() {
        return "안녕하세요. 무엇을 도와드릴까요?\n- 오늘 먹을 약 뭐야\n- 오늘 아침 약 먹었어";
    }

    private String appIntroReply() {
        return "메디태그는 시각장애인을 위한 복약 도우미 앱입니다. "
                + "시간대별 알림으로 약 복용을 안내하고, 음성 기반 챗봇으로 등록·조회·기록을 간편하게 할 수 있어요. "
                + "OCR로 약 봉투 글자 인식도 지원하며, 복용 기록 시 보호자에게 알림을 보낼 수 있습니다. "
                + "원하시면 오늘 복용 일정을 알려드릴까요?";
    }
}
