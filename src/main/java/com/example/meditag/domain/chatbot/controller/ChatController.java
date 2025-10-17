package com.example.meditag.domain.chatbot.controller;

import com.example.meditag.domain.auth.dto.CustomUserDetails;
import com.example.meditag.domain.chatbot.controller.api.ChatApi;
import com.example.meditag.domain.chatbot.dto.ChatSessionDTO;
import com.example.meditag.domain.chatbot.dto.MessageDTO;
import com.example.meditag.domain.chatbot.entity.ChatSession;
import com.example.meditag.domain.chatbot.entity.Message;
import com.example.meditag.domain.chatbot.repository.MessageRepository;
import com.example.meditag.domain.chatbot.service.ChatSessionService;
import com.example.meditag.domain.chatbot.service.message.MessageProcessorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController // 이 클래스가 REST API 컨트롤러임을 나타냄
@RequestMapping("/api/chat") // 이 컨트롤러의 모든 요청 URL 앞에 "/api/chat"이 붙음
@RequiredArgsConstructor // final 필드를 자동으로 생성자로 주입해줌 (DI)
public class ChatController implements ChatApi {

    private final ChatSessionService chatSessionService; // 채팅 세션 생성 및 조회 서비스
    private final MessageProcessorService messageProcessorService; // 메시지를 처리하는 핵심 로직 서비스
    private final MessageRepository messageRepository; // 메시지를 DB에서 조회 및 저장하는 JPA 리포지토리

    @PostMapping("/session") // POST /api/chat/session 요청을 처리 (채팅 세션 생성)
    public ResponseEntity<ChatSessionDTO> createChatSession(@AuthenticationPrincipal CustomUserDetails user) {
        // 로그인한 사용자의 username으로 새로운 채팅 세션 생성
        ChatSessionDTO session = chatSessionService.createChatSession(user.getUsername());
        // 생성된 세션 정보를 HTTP 200 OK 응답으로 반환
        return ResponseEntity.ok(session);
    }

    @GetMapping("/session") // GET /api/chat/session 요청을 처리 (사용자의 채팅 세션 목록 조회)
    public ResponseEntity<List<ChatSessionDTO>> getSessions(@AuthenticationPrincipal CustomUserDetails user) {
        // 로그인한 사용자의 모든 채팅 세션 리스트 반환
        return ResponseEntity.ok(chatSessionService.getSessionsByMemberId(user.getUsername()));
    }

    @PostMapping("/message/{chatSessionId}") // POST /api/chat/message/{chatSessionId} 요청 처리 (메시지 전송 및 응답 생성)
    public ResponseEntity<MessageDTO> processMessage(
            @AuthenticationPrincipal CustomUserDetails user, // 인증된 사용자 정보
            @PathVariable Long chatSessionId, // URL 경로에서 채팅 세션 ID 추출
            @RequestBody MessageDTO request // 사용자가 보낸 메시지 본문 (JSON -> 객체로 바인딩)
    ) {
        // 메시지를 처리하고 챗봇 응답을 생성
        String responseContent = messageProcessorService.processMessage(
                user.getUsername(), // 사용자 이름
                chatSessionId, // 채팅 세션 ID
                request.getContent() // 사용자가 입력한 메시지 내용
        );

        // 챗봇 응답 메시지를 DB에 저장
        Message savedMessage = messageRepository.save(Message.builder()
                .sender(Message.Sender.BOT) // 보낸 사람: 챗봇
                .chatSession(ChatSession.builder().id(chatSessionId).build()) // 어떤 세션에 속한 메시지인지 설정
                .content(responseContent) // GPT나 기능 처리 후 응답 내용
                .build());

        // 응답으로 메시지 DTO 반환 (sender와 content만 포함)
        return ResponseEntity.ok(MessageDTO.builder()
                .sender(savedMessage.getSender().name()) // BOT
                .content(savedMessage.getContent()) // 응답 내용
                .build());
    }

    @GetMapping("/message/{chatSessionId}") // GET /api/chat/message/{chatSessionId} 요청 처리 (채팅 내역 조회)
    public ResponseEntity<List<MessageDTO>> getMessages(
            @AuthenticationPrincipal CustomUserDetails user, // 인증된 사용자 정보
            @PathVariable Long chatSessionId // 채팅 세션 ID
    ) {
        // 해당 세션의 모든 메시지를 조회하여 DTO로 변환
        List<MessageDTO> messages = messageRepository.findByChatSessionId(chatSessionId)
                .stream()
                .map(m -> MessageDTO.builder()
                        .sender(m.getSender().name()) // USER or BOT
                        .content(m.getContent()) // 메시지 내용
                        .build())
                .collect(Collectors.toList());

        // 메시지 리스트를 HTTP 200 OK 응답으로 반환
        return ResponseEntity.ok(messages);
    }
}
