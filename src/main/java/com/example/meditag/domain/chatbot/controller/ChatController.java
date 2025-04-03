package com.example.meditag.domain.chatbot.controller;

import com.example.meditag.domain.auth.dto.CustomUserDetails;
import com.example.meditag.domain.chatbot.controller.api.ChatApi;
import com.example.meditag.domain.chatbot.dto.ChatSessionDTO;
import com.example.meditag.domain.chatbot.dto.MessageDTO;
import com.example.meditag.domain.chatbot.service.ChatSessionService;
import com.example.meditag.domain.chatbot.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController implements ChatApi {

    private final ChatSessionService chatSessionService;
    private final MessageService messageService;

    // 채팅 시작
    @PostMapping("/session")
    public ResponseEntity<String> createChatSession(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        chatSessionService.createChatSession(customUserDetails.getUsername());
        return ResponseEntity.ok("채팅 시작!");
    }

    // 사용자 세션 조회
    @GetMapping("/session")
    public ResponseEntity<List<ChatSessionDTO>> getSessions(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        List<ChatSessionDTO> sessionsByMemberId = chatSessionService.getSessionsByMemberId(customUserDetails.getUsername());
        return ResponseEntity.ok(sessionsByMemberId);
    }

    // 메시지 저장 및 응답 생성 (통합 API)
    @PostMapping("/message/{chatSessionId}")
    public ResponseEntity<MessageDTO> saveMessageAndGenerateResponse(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                                     @PathVariable Long chatSessionId,
                                                                     @RequestBody MessageDTO messageDto) {
        // 사용자 메시지 저장 및 챗봇 응답 생성
        MessageDTO responseMessage = messageService.saveMessageAndGenerateResponse(
                customUserDetails.getUsername(),
                chatSessionId,
                messageDto
        );

        return ResponseEntity.ok(responseMessage);
    }


    // 대화 기록 조회
    @GetMapping("/message/{chatSessionId}")
    public ResponseEntity<List<MessageDTO>> getMessages(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                        @PathVariable Long chatSessionId) {
        List<MessageDTO> messagesByChatSessionId = messageService.getMessagesByChatSessionId(customUserDetails.getUsername(), chatSessionId);
        return ResponseEntity.ok(messagesByChatSessionId);
    }
}
