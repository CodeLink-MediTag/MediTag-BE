package com.example.meditag.domain.chatbot.controller.api;

import com.example.meditag.domain.auth.dto.CustomUserDetails;
import com.example.meditag.domain.chatbot.dto.ChatSessionDTO;
import com.example.meditag.domain.chatbot.dto.MessageDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    @Operation(summary = "채팅 세션 생성", description = "사용자가 새로운 채팅 세션을 시작합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "채팅 세션 생성 성공",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    ResponseEntity<String> createChatSession(
            @Parameter(description = "사용자 정보") @AuthenticationPrincipal CustomUserDetails customUserDetails
    );

    @Operation(summary = "사용자 세션 조회", description = "사용자의 모든 채팅 세션을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "채팅 세션 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ChatSessionDTO.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    ResponseEntity<List<ChatSessionDTO>> getSessions(
            @Parameter(description = "사용자 정보") @AuthenticationPrincipal CustomUserDetails customUserDetails
    );

    @Operation(summary = "메시지 저장 및 응답 생성", description = "사용자가 보낸 메시지를 저장하고 챗봇 응답을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "메시지 저장 및 응답 생성 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageDTO.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    ResponseEntity<MessageDTO> saveMessageAndGenerateResponse(
            @Parameter(description = "사용자 정보") @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long chatSessionId,
            @RequestBody(description="사용자가 보낸 메시지 정보", required=true,
                    content=@Content(mediaType="application/json",
                            schema=@Schema(implementation=MessageDTO.class))) MessageDTO messageDto);

    @Operation(summary = "대화 기록 조회", description = "특정 채팅 세션의 대화 기록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "대화 기록 조회 성공",
                    content=@Content(mediaType="application/json",
                            schema=@Schema(implementation=MessageDTO.class))),
            @ApiResponse(responseCode = "404", description="채팅 세션을 찾을 수 없음", content=@Content),
            @ApiResponse(responseCode="401", description="인증 실패", content=@Content)
    })
    ResponseEntity<List<MessageDTO>> getMessages(
            @Parameter(description = "사용자 정보") @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long chatSessionId);
}
