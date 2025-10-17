// src/main/java/com/example/meditag/domain/chatbot/service/OpenAiService.java
package com.example.meditag.domain.chatbot.service;

import com.example.meditag.domain.chatbot.dto.OpenAiResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenAiService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    /** 기존 메서드는 유지하되, 실패 시 null을 반환하도록 완화(던지지 않음). */
    public String sendMessageToOpenAi(String prompt) {
        try {
            String systemPrompt = """
            당신은 사용자의 질문에 신뢰할 수 있는 정보를 바탕으로만 답변하는 AI입니다.
            (중략: 내부 원칙)
            """;

            String response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(Map.of(
                            "model", "gpt-3.5-turbo",
                            "messages", List.of(
                                    Map.of("role", "system", "content", systemPrompt),
                                    Map.of("role", "user", "content", prompt)
                            ),
                            "max_tokens", 500,
                            "temperature", 0.7
                    ))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            OpenAiResponseDTO openAiResponse = objectMapper.readValue(response, OpenAiResponseDTO.class);
            return openAiResponse.getChoices().get(0).getMessage().getContent().trim();

        } catch (Exception e) {
            // 절대 throw하지 말고 null을 반환해 상위에서 폴백
            return null;
        }
    }

    /** 스몰토크 등 가벼운 대화용: 실패해도 기본 응답을 리턴 */
    public String trySmallTalk(String userMessage) {
        String ans = sendMessageToOpenAi(userMessage);
        if (ans == null || ans.isBlank()) {
            return "안녕하세요! 무엇을 도와드릴까요?";
        }
        return ans;
    }
}
