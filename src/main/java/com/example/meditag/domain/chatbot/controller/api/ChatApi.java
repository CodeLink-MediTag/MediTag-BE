package com.example.meditag.domain.chatbot.controller.api;

import com.example.meditag.domain.auth.dto.CustomUserDetails;
import com.example.meditag.domain.chatbot.dto.ChatSessionDTO;
import com.example.meditag.domain.chatbot.dto.MessageDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Tag(name = "챗봇 관리", description = "챗봇 관련 API")
public interface ChatApi {

    @Operation(summary = "채팅 세션 생성", description = "로그인한 사용자가 새로운 채팅 세션을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "채팅 세션 생성 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ChatSessionDTO.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    ResponseEntity<ChatSessionDTO> createChatSession(@AuthenticationPrincipal CustomUserDetails customUserDetails);

    @Operation(summary = "사용자 세션 조회", description = "로그인한 사용자의 모든 채팅 세션 목록을 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "채팅 세션 목록 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ChatSessionDTO.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    ResponseEntity<List<ChatSessionDTO>> getSessions(@AuthenticationPrincipal CustomUserDetails customUserDetails);

    @Operation(summary = "메시지 전송 및 응답 생성", description = "사용자가 입력한 메시지를 처리하고 챗봇의 응답 메시지를 반환합니다.\n" +
            "```json\n" +
            "{\n" +
            "  \"sender\": \"USER\",\n" +
            "  \"content\": \"질문~~\"\n" +
            "}\n" +
            "```")

    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "응답 메시지 생성 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageDTO.class))),
            @ApiResponse(responseCode = "400", description = "요청 데이터 오류", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    ResponseEntity<MessageDTO> processMessage(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long chatSessionId,
            @RequestBody(description = "사용자 입력 메시지",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageDTO.class)))
            MessageDTO messageDto);

    @Operation(summary = "대화 메시지 조회", description = "지정한 채팅 세션의 전체 대화 메시지를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "대화 메시지 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageDTO.class))),
            @ApiResponse(responseCode = "404", description = "세션 없음", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    ResponseEntity<List<MessageDTO>> getMessages(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long chatSessionId);
}
