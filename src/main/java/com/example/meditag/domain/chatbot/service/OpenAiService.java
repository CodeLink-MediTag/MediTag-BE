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

    private final WebClient webClient; // WebClient를 통한 OpenAI API 통신
    private final ObjectMapper objectMapper; // JSON 파싱용 객체

    public String sendMessageToOpenAi(String prompt) {
        try {
            String systemPrompt = """
            당신은 사용자의 질문에 신뢰할 수 있는 정보를 바탕으로만 답변하는 AI입니다.
            다음 규칙을 반드시 따르세요:
            1) 충분한 근거가 없거나 정보가 불확실한 경우, 절대 임의로 지어내지 말고 "알 수 없습니다" 또는 "잘 모르겠습니다"라고 명시하세요.
            2) 답변 전 단계별로 정보를 검증하고, 모호하거나 출처가 불분명한 경우 "확실하지 않음"으로 표시하세요.
            3) 확실한 정보만으로 간결한 답변을 생성하세요. 추측이 필요한 경우 "추측입니다"라고 반드시 밝혀주세요.
            4) 질문이 모호하거나 정보가 부족하면 먼저 세부 정보를 요청하세요.
            5) 확인되지 않은 사실은 단정적으로 말하지 말고, 근거가 있다면 함께 제시하세요.
            6) 출처가 있다면 간단히 요약하여 포함하세요.
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
            e.printStackTrace();
            throw new RuntimeException("OpenAI 응답 파싱 중 오류 발생", e);
        }
    }

}

