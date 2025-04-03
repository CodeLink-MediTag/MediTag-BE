package com.example.meditag.domain.chatbot.service;

import com.example.meditag.domain.alarm.entity.Alarm;
import com.example.meditag.domain.alarm.repository.AlarmRepository;
import com.example.meditag.domain.chatbot.dto.DailyUserStatus;
import com.example.meditag.domain.chatbot.dto.MessageDTO;
import com.example.meditag.domain.chatbot.entity.ChatSession;
import com.example.meditag.domain.chatbot.entity.FAQ;
import com.example.meditag.domain.chatbot.entity.Message;
import com.example.meditag.domain.chatbot.repository.MessageRepository;
import com.example.meditag.domain.member.entity.Member;
import com.example.meditag.domain.member.repository.MemberRepository;
import com.example.meditag.global.error.exception.CustomException;
import com.example.meditag.global.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    private final MessageRepository messageRepository;
    private final MemberRepository memberRepository;
    private final FAQService faqService;
    private final OpenAiService openAiService;
    private final DailyStatusService dailyStatusService;
    private final AlarmRepository alarmRepository;
    private final HealthFeatureService healthFeatureService;

    public MessageDTO saveMessageAndGenerateResponse(String username, Long chatSessionId, MessageDTO userMessageDto) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Message userMessage = Message.builder()
                .sender(Message.Sender.USER)
                .content(userMessageDto.getContent())
                .chatSession(ChatSession.builder().id(chatSessionId).build())
                .build();

        messageRepository.save(userMessage);

        List<Message> previousMessages = messageRepository.findByChatSessionId(chatSessionId);
        String conversationHistory = previousMessages.stream()
                .map(msg -> (msg.getSender().equals(Message.Sender.USER) ? "사용자: " : "챗봇: ") + msg.getContent())
                .collect(Collectors.joining("\n"));

        FAQ closestFAQ = faqService.findClosestQuestion(userMessageDto.getContent());

        String botResponse;
        MessageDTO.ActionDTO action = MessageDTO.ActionDTO.builder()
                .type(MessageDTO.ActionDTO.ActionType.NONE)
                .target(null)
                .build();

        if (userMessageDto.getContent().contains("복용 여부 변경") || userMessageDto.getContent().matches(".*(아침|점심|저녁).*복용.*변경.*")) {
            // GPT를 통해 시간대 인식
            String instructionPrompt = buildInstructionPrompt(userMessageDto.getContent());
            String timeResult = openAiService.sendMessageToOpenAi(instructionPrompt).trim();

            if (List.of("아침", "점심", "저녁").contains(timeResult)) {
                // 오늘 날짜의 특정 시간대 약 알림 조회
                LocalDate today = LocalDate.now();
                List<Alarm> alarmsByTime = alarmRepository.findByUsernameAndDosageTimeAndDate(
                        username, timeResult, today.atStartOfDay(), today.plusDays(1).atStartOfDay().minusSeconds(1)
                );

                if (alarmsByTime.isEmpty()) {
                    botResponse = "오늘 " + timeResult + " 시간대에는 복용할 약이 없어요.";
                } else {
                    for (Alarm alarm : alarmsByTime) {
                        alarm.toggleTaking();
                        alarmRepository.save(alarm);
                    }
                    botResponse = "오늘 " + timeResult + "약의 복용 상태를 변경했어요! 잘하셨어요 😊";
                }
            } else {
                botResponse = "어느 시간대의 약인지 잘 모르겠어요. '아침약 복용했어'처럼 다시 알려주세요!";
            }
        } else if (userMessageDto.getContent().matches(".*\\d{1,2}월 \\d{1,2}일.*약.*")) {
            LocalDate date = extractDateFromMessage(userMessageDto.getContent());

            List<Alarm> alarms = alarmRepository.findByUsernameAndAlarmDate(
                    username,
                    date.atStartOfDay(),
                    date.plusDays(1).atStartOfDay().minusSeconds(1)
            );

            if (alarms.isEmpty()) {
                botResponse = date.format(DateTimeFormatter.ofPattern("M월 d일")) + "에는 복용할 약이 없습니다.";
            } else {
                String prompt = buildPromptForSpecificDate(date, alarms);
                botResponse = openAiService.sendMessageToOpenAi(prompt);
            }
        } else if (closestFAQ != null && faqService.getSimilarityScore(userMessageDto.getContent(), closestFAQ.getQuestion()) <= 10) {
            DailyUserStatus status = dailyStatusService.getFullStatus(username);
            List<Alarm> alarms = alarmRepository.findAllAlarmsByUsername(username);
            String prompt = buildPromptWithHistory(userMessageDto.getContent(), status, alarms, conversationHistory);

            log.info("📢 생성된 프롬프트:\n{}", prompt);

            botResponse = openAiService.sendMessageToOpenAi(prompt);
        } else {
            botResponse = openAiService.sendMessageToOpenAi(conversationHistory + "\n사용자: " + userMessageDto.getContent());
        }

        Message botMessage = Message.builder()
                .sender(Message.Sender.BOT)
                .content(botResponse)
                .chatSession(ChatSession.builder().id(chatSessionId).build())
                .build();

        messageRepository.save(botMessage);

        return MessageDTO.builder()
                .sender("BOT")
                .content(botResponse)
                .action(action)
                .build();
    }

    private LocalDate extractDateFromMessage(String message) {
        Pattern pattern = Pattern.compile("(\\d{1,2})월 (\\d{1,2})일");
        Matcher matcher = pattern.matcher(message);

        if (matcher.find()) {
            int month = Integer.parseInt(matcher.group(1));
            int day = Integer.parseInt(matcher.group(2));
            return LocalDate.of(LocalDate.now().getYear(), month, day);
        } else {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private String buildPromptForSpecificDate(LocalDate date, List<Alarm> alarms) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("당신은 사용자의 복약을 도와주는 건강 비서입니다.\n")
                .append(date.format(DateTimeFormatter.ofPattern("M월 d일")))
                .append("의 복약 일정입니다. 아래 데이터를 기반으로 친절하고 정확하게 답변해주세요.\n\n")
                .append("- 약 이름과 복용 시간은 간단하고 알아듣기 쉽게 말해주세요.\n")
                .append("- 복용 전인 약을 강조해주세요.\n")
                .append("- 복용 완료한 약은 ‘잘 하셨어요!’ 같은 격려도 해주세요.\n")
                .append("- 말투는 따뜻하고 자연스럽게, 사용자에게 친구처럼 말해주세요.\n\n");

        alarms.stream()
                .sorted(Comparator.comparing(Alarm::getAlarmTime))
                .forEach(alarm -> prompt.append("- 약: ").append(alarm.getCalendar().getMedicine().getName())
                        .append(" (시간: ").append(alarm.getAlarmTime().format(DateTimeFormatter.ofPattern("HH:mm"))).append(")\n"));

        return prompt.toString();
    }

    private String buildInstructionPrompt(String message) {
        return """
        사용자가 약 복용 상태를 변경하고 싶어 합니다.
        사용자가 말한 내용에서 '아침', '점심', '저녁' 중 어떤 시간대의 약을 말하는지 하나만 알려주세요.
        애매하면 '없음'이라고 대답해주세요.

        예시:
        - 아침약 먹었어 → 아침
        - 점심 약 복용 완료 → 점심
        - 저녁약 상태 바꿔줘 → 저녁
        - 약 먹었어 → 없음

        [사용자 메시지]: "%s"
        """.formatted(message);
    }


    private String buildPromptWithHistory(String question, DailyUserStatus status, List<Alarm> alarms, String conversationHistory) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("당신은 사용자의 복약을 도와주는 건강 비서입니다.\n")
                .append("- 약 이름과 복용 시간은 간단하고 알아듣기 쉽게 말해주세요.\n")
                .append("- 복용 전인 약을 강조해주세요.\n")
                .append("[이전 대화 기록]\n")
                .append(conversationHistory)
                .append("\n\n");

        prompt.append("[오늘 복약 정보]\n");
        alarms.stream()
                .sorted(Comparator.comparing(Alarm::getAlarmTime))
                .forEach(alarm -> {
                    String medicineName = alarm.getCalendar().getMedicine().getName();
                    String characteristic = alarm.getCalendar().getMedicine().getCharacteristic();
                    String alarmTimeStr = alarm.getAlarmTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    prompt.append("- 약: ").append(medicineName).append("\n")
                            .append("  특징: ").append(characteristic).append("\n")
                            .append("  알림 시간: ").append(alarmTimeStr).append(" / 복용 ")
                            .append(alarm.isTaking() ? "완료" : "전").append("\n\n");
                });

        prompt.append("\n[사용자 질문] ").append(question);
        return prompt.toString();
    }

    private Long extractAlarmId(String messageContent) {
        try {
            return Long.parseLong(messageContent.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private String buildPrompt(String question, DailyUserStatus status, List<Alarm> alarms) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("당신은 사용자의 복약을 도와주는 건강 비서입니다.\n")
                .append("- 약 이름과 복용 시간은 간단하고 알아듣기 쉽게 말해주세요.\n")
                .append("- 복용 전인 약을 강조해주세요.\n")
                .append("- 복용 완료한 약은 ‘잘 하셨어요!’ 같은 격려도 해주세요.\n")
                .append("- 말투는 따뜻하고 자연스럽게, 사용자에게 친구처럼 말해주세요.\n\n")
                .append("오늘 날짜: ").append(status.getDate()).append("\n\n")
                .append("[복약 정보 - 전체 알람 기준]\n");

        alarms.stream()
                .sorted(Comparator.comparing(Alarm::getAlarmTime))
                .forEach(alarm -> {
                    String medicineName = alarm.getCalendar().getMedicine().getName();
                    String characteristic = alarm.getCalendar().getMedicine().getCharacteristic();
                    String alarmTimeStr = alarm.getAlarmTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    prompt.append("- 약: ").append(medicineName).append("\n")
                            .append("  특징: ").append(characteristic).append("\n")
                            .append("  알림 시간: ").append(alarmTimeStr).append(" / 복용 ")
                            .append(alarm.isTaking() ? "완료" : "전").append("\n\n");
                });

        prompt.append("[녹음 기록]\n");
        if (status.getRecordings().isEmpty()) {
            prompt.append("- 녹음 없음\n");
        } else {
            status.getRecordings().forEach(rec ->
                    prompt.append("- 제목: ").append(rec.getTitle())
                            .append(", 시간: ").append(rec.getRecordingTime()).append("\n")
            );
        }

        prompt.append("\n[사용자 질문] ").append(question).append("\n");
        return prompt.toString();
    }

    public List<MessageDTO> getMessagesByChatSessionId(String username, Long chatSessionId) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        return messageRepository.findByChatSessionId(chatSessionId).stream()
                .map(message -> MessageDTO.builder()
                        .sender(message.getSender().name())
                        .content(message.getContent())
                        .build())
                .collect(Collectors.toList());
    }
}
