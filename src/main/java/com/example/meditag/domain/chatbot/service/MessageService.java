package com.example.meditag.domain.chatbot.service;

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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final MemberRepository memberRepository;
    private final FAQService faqService;
    private final OpenAiService openAiService;

    public List<MessageDTO> getMessagesByChatSessionId(String username, Long chatSessionId) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        return messageRepository.findByChatSessionId(chatSessionId)
                .stream()
                .map(message -> MessageDTO.builder()
                        .sender(message.getSender().name())
                        .content(message.getContent())
                        .build())
                .collect(Collectors.toList());
    }

    public MessageDTO saveMessageAndGenerateResponse(String username, Long chatSessionId, MessageDTO userMessageDto) {
        // 사용자 확인
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 사용자 메시지 저장
        Message userMessage = Message.builder()
                .sender(Message.Sender.USER)
                .content(userMessageDto.getContent())
                .chatSession(ChatSession.builder().id(chatSessionId).build())
                .build();

        messageRepository.save(userMessage);

        // 챗봇 응답 생성
        String botResponse = openAiService.sendMessageToOpenAi(userMessageDto.getContent());

        // 챗봇 메시지 저장
        Message botMessage = Message.builder()
                .sender(Message.Sender.BOT)
                .content(botResponse)
                .chatSession(ChatSession.builder().id(chatSessionId).build())
                .build();
        messageRepository.save(botMessage);

        // 응답 DTO 생성
        return MessageDTO.builder()
                .sender("BOT")
                .content(botResponse)
                .build();
    }

    public String generateResponse(String userMessage) {
        // FAQ에서 답변 검색
        FAQ closestFAQ = faqService.findClosestQuestion(userMessage);
        if (closestFAQ != null) {
            return closestFAQ.getAnswer();
        }

        // OpenAI API 호출 (FAQ에 없는 경우)
        return openAiService.sendMessageToOpenAi("User: " + userMessage);
    }
}
