package com.example.meditag.domain.chatbot.service.message;

import com.example.meditag.domain.chatbot.entity.Message;
import com.example.meditag.domain.chatbot.repository.MessageRepository;
import com.example.meditag.domain.chatbot.service.OpenAiService;
import com.example.meditag.domain.alarm.entity.Alarm;
import com.example.meditag.domain.alarm.repository.AlarmRepository;
import com.example.meditag.domain.calendar.entity.Calendar;
import com.example.meditag.domain.medicine.entity.Medicine;
import com.example.meditag.domain.medicine.repository.MedicineRepository;
import com.example.meditag.global.error.exception.CustomException;
import com.example.meditag.global.error.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GPTAnswerService {

    private final MessageRepository messageRepository;
    private final OpenAiService openAiService;
    private final MedicineRepository medicineRepository;
    private final AlarmRepository alarmRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isApplicable(String message) {
        return true;
    }

    public String execute(String username, Long chatSessionId, String message) {
        List<Message> previousMessages = messageRepository.findByChatSessionId(chatSessionId);
        String conversationHistory = previousMessages.stream()
                .map(m -> (m.getSender() == Message.Sender.USER ? "사용자: " : "챗봇: ") + m.getContent())
                .collect(Collectors.joining("\n"));

        List<Medicine> medicines = medicineRepository.findByMember_Username(username);
        List<Alarm> alarms = alarmRepository.findByCalendar_Medicine_Member_Username(username);

        LocalDate today = LocalDate.now();

        if (message.matches(".*(다른|또).*아침약.*(있어|남았어|먹어야).*")) {
            List<Alarm> morningAlarms = alarms.stream()
                    .filter(alarm -> alarm.getCalendar().getDate().isEqual(today))
                    .filter(alarm -> alarm.getDosageTime().equals("아침"))
                    .collect(Collectors.toList());

            List<Alarm> pendingMorningAlarms = morningAlarms.stream()
                    .filter(alarm -> !alarm.isTaking())
                    .collect(Collectors.toList());

            if (pendingMorningAlarms.isEmpty()) {
                return "오늘 아침에 복용하실 약을 모두 복용하셨습니다! 잘하셨어요 😊";
            }

            String pendingMedicines = pendingMorningAlarms.stream()
                    .map(alarm -> alarm.getCalendar().getMedicine().getName())
                    .distinct()
                    .collect(Collectors.joining(", "));

            return String.format("오늘 아침에 아직 복용하지 않은 약은 다음과 같아요: %s입니다. 꼭 챙겨드세요! 🌿", pendingMedicines);
        }

        if (isNextMedicineRequest(message)) {
            return getNextMedicineMessage(alarms);
        }

        if (isTodayInquiry(message)) {
            return getTodayMedicineMessage(alarms, message);
        }

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("너는 친절한 복약 관리 도우미야.\n")
                .append("아래는 사용자의 등록된 약 정보야:\n\n");

        medicines.forEach(med -> promptBuilder
                .append("약 이름: ").append(med.getName())
                .append("\n특징: ").append(med.getCharacteristic())
                .append("\n복용 기간: ").append(med.getStartDate()).append(" ~ ")
                .append(med.getStartDate().plusDays(med.getDuration() - 1))
                .append("\n처방 여부: ").append(med.isPrescribed() ? "처방약" : "비처방약")
                .append("\n\n"));

        promptBuilder.append("복용 알람 정보:\n");
        alarms.forEach(alarm -> {
            Calendar calendar = alarm.getCalendar();
            promptBuilder.append("- ").append(calendar.getDate())
                    .append(" / 약: ").append(calendar.getMedicine().getName())
                    .append(" / 시간: ").append(alarm.getAlarmTime().toLocalTime())
                    .append(" / 상태: ").append(alarm.isTaking() ? "복용 완료" : "복용 전")
                    .append("\n");
        });

        promptBuilder.append("\n사용자 질문: ").append(message);

        try {
            String response = openAiService.sendMessageToOpenAi(promptBuilder.toString());
            JsonNode json = objectMapper.readTree(response);
            return json.get("choices").get(0).get("message").get("content").asText();
        } catch (Exception e) {
            log.error("OpenAI 응답 파싱 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.OPENAI_PARSE_ERROR);
        }
    }

    private boolean isNextMedicineRequest(String message) {
        return message.matches(".*(다음|곧|지금|복용할 차례|언제).*");
    }

    private boolean isTodayInquiry(String message) {
        return message.matches(".*(오늘|지금|몇시|언제|먹어야).*");
    }

    private String getNextMedicineMessage(List<Alarm> alarms) {
        LocalDateTime now = LocalDateTime.now();
        return alarms.stream()
                .filter(alarm -> alarm.getAlarmTime().isAfter(now))
                .sorted(Comparator.comparing(Alarm::getAlarmTime))
                .findFirst()
                .map(alarm -> String.format("다음 복용 약은 '%s'이고, 시간은 %s입니다. 복용 상태는 %s입니다.",
                        alarm.getCalendar().getMedicine().getName(),
                        alarm.getAlarmTime().toLocalTime(),
                        alarm.isTaking() ? "이미 복용 완료" : "아직 복용 전"))
                .orElse("오늘 더 이상 복용할 약이 없습니다. 편안히 쉬세요! 😊");
    }

    private String getTodayMedicineMessage(List<Alarm> alarms, String message) {
        LocalDate today = LocalDate.now();
        List<Alarm> todayAlarms = alarms.stream()
                .filter(alarm -> alarm.getCalendar().getDate().isEqual(today))
                .sorted(Comparator.comparing(Alarm::getAlarmTime))
                .toList();

        if (todayAlarms.isEmpty()) {
            return "오늘은 복용해야 할 약이 없어요. 😊";
        }

        StringBuilder todayPrompt = new StringBuilder();
        todayPrompt.append("오늘의 복약 일정입니다:\n");
        todayAlarms.forEach(alarm -> todayPrompt
                .append("- 약: ").append(alarm.getCalendar().getMedicine().getName())
                .append(" / 시간: ").append(alarm.getAlarmTime().toLocalTime())
                .append(" / 상태: ").append(alarm.isTaking() ? "복용 완료 ✅" : "복용 전 🔔")
                .append("\n"));

        todayPrompt.append("\n사용자 질문: ").append(message);

        try {
            String response = openAiService.sendMessageToOpenAi(todayPrompt.toString());
            JsonNode json = objectMapper.readTree(response);
            return json.get("choices").get(0).get("message").get("content").asText();
        } catch (Exception e) {
            log.error("OpenAI 응답 파싱 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.OPENAI_PARSE_ERROR);
        }
    }
}