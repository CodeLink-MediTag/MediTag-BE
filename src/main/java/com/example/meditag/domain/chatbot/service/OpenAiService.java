// src/main/java/com/example/meditag/domain/chatbot/service/OpenAiService.java
package com.example.meditag.domain.chatbot.service;

import com.example.meditag.domain.chatbot.dto.OpenAiResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    private static final String DEFAULT_MODEL = "gpt-3.5-turbo";
    private static final int    DEFAULT_MAX_TOKENS = 500;
    private static final double DEFAULT_TEMPERATURE = 0.7;

    private static final String DEFAULT_SYSTEM_PROMPT = """
        당신은 시각장애인 사용자를 돕는 복약 도우미 앱 '메디태그'의 내장 비서입니다.
        사실에 근거하여 간결하고 공손하게 답하세요.
        명시적인 '기록/변경/체크/토글/등록/삭제' 요청이 없으면 DB 변경을 제안하지 않습니다.
        모호하면 추가 질문은 1개만 하세요.
        """;

    public String sendMessageToOpenAi(String userPrompt) {
        return sendMessageWithSystem(DEFAULT_SYSTEM_PROMPT, userPrompt, DEFAULT_MODEL, DEFAULT_TEMPERATURE, DEFAULT_MAX_TOKENS);
    }

    public String sendMessageWithSystem(String systemPrompt, String userPrompt) {
        return sendMessageWithSystem(systemPrompt, userPrompt, DEFAULT_MODEL, DEFAULT_TEMPERATURE, DEFAULT_MAX_TOKENS);
    }

    public String sendMessageWithSystem(String systemPrompt,
                                        String userPrompt,
                                        String model,
                                        double temperature,
                                        int maxTokens) {
        try {
            String response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(Map.of(
                            "model", model,
                            "messages", List.of(
                                    Map.of("role", "system", "content", systemPrompt == null ? "" : systemPrompt),
                                    Map.of("role", "user", "content", userPrompt == null ? "" : userPrompt)
                            ),
                            "max_tokens", maxTokens,
                            "temperature", temperature
                    ))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null || response.isBlank()) {
                return null;
            }

            OpenAiResponseDTO dto = objectMapper.readValue(response, OpenAiResponseDTO.class);
            if (dto.getChoices() == null || dto.getChoices().isEmpty()
                    || dto.getChoices().get(0).getMessage() == null) {
                return null;
            }
            String content = dto.getChoices().get(0).getMessage().getContent();
            return content == null ? null : content.trim();

        } catch (Exception e) {
            log.warn("OpenAI call failed: {}", e.toString());
            return null; // 실패 시 항상 null 반환
        }
    }

    public String trySmallTalk(String userMessage) {
        return sendMessageToOpenAi(userMessage);
    }
}
