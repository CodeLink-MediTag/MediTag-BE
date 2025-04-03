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
            String response = webClient.post() // POST 요청 생성
                    .uri("/chat/completions") // 요청 URI
                    .bodyValue(Map.of( // 요청 본문 설정
                            "model", "gpt-3.5-turbo",
                            "messages", List.of(Map.of("role", "user", "content", prompt)),
                            "max_tokens", 500,
                            "temperature", 0.7
                    ))
                    .retrieve() // 응답 받기
                    .bodyToMono(String.class)
                    .block(); // 동기 방식으로 결과 대기

            OpenAiResponseDTO openAiResponse = objectMapper.readValue(response, OpenAiResponseDTO.class); // JSON 파싱
            return openAiResponse.getChoices().get(0).getMessage().getContent().trim(); // 응답 텍스트 반환

        } catch (Exception e) {
            e.printStackTrace(); // 에러 출력
            throw new RuntimeException("OpenAI 응답 파싱 중 오류 발생", e); // 예외 던짐
        }
    }
}

