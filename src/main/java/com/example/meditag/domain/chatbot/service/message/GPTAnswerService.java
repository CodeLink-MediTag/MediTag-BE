package com.example.meditag.domain.chatbot.service.message;

import com.example.meditag.domain.chatbot.entity.Message;
import com.example.meditag.domain.chatbot.repository.MessageRepository;
import com.example.meditag.domain.chatbot.service.OpenAiService;
import com.example.meditag.domain.alarm.entity.Alarm;
import com.example.meditag.domain.alarm.repository.AlarmRepository;
import com.example.meditag.domain.calendar.entity.Calendar;
import com.example.meditag.domain.medicine.entity.Medicine;
import com.example.meditag.domain.medicine.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GPTAnswerService {

    private final MessageRepository messageRepository;
    private final OpenAiService openAiService;
    private final MedicineRepository medicineRepository;
    private final AlarmRepository alarmRepository;

    public boolean isApplicable(String message) {
        return true;
    }

    public String execute(String username, Long chatSessionId, String message) {
        // 1. 대화 히스토리 불러오기
        List<Message> previousMessages = messageRepository.findByChatSessionId(chatSessionId);
        String conversationHistory = previousMessages.stream()
                .map(m -> (m.getSender() == Message.Sender.USER ? "사용자: " : "챗봇: ") + m.getContent())
                .collect(Collectors.joining("\n"));

        // 2. 사용자 약 및 알람 정보 가져오기
        List<Medicine> medicines = medicineRepository.findByMember_Username(username);
        List<Alarm> alarms = alarmRepository.findByCalendar_Medicine_Member_Username(username);

        // 3. 데이터 기반 프롬프트 생성
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("너는 사용자 복약 관리 도우미야.\n")
                .append("아래는 사용자의 등록된 약물 및 복용 정보야:\n\n");

        for (Medicine med : medicines) {
            promptBuilder.append("약 이름: ").append(med.getName()).append("\n")
                    .append(" - 특징: ").append(med.getCharacteristic()).append("\n")
                    .append(" - 복용 시작일: ").append(med.getStartDate()).append("\n")
                    .append(" - 복용 기간: ").append(med.getDuration()).append("일\n")
                    .append(" - 처방약 여부: ").append(med.isPrescribed() ? "처방약" : "비처방약").append("\n\n");
        }

        promptBuilder.append("복용 일정 및 알람 정보:\n");

        for (Alarm alarm : alarms) {
            Calendar calendar = alarm.getCalendar();
            String medicineName = calendar.getMedicine().getName();

            promptBuilder.append(" - 날짜: ").append(calendar.getDate())
                    .append(" / 약: ").append(medicineName)
                    .append(" / 시간: ").append(alarm.getDosageTime())
                    .append(" / 복용 여부: ").append(alarm.isTaking() ? "복용 완료" : "복용 전")
                    .append("\n");
        }

        promptBuilder.append("\n위 정보를 참고해서 아래 질문에 답변해줘:\n");
        promptBuilder.append("사용자: ").append(message).append("\n");

        // 4. GPT 호출
        return openAiService.sendMessageToOpenAi(promptBuilder.toString());
    }
}
