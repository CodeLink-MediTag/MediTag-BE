package com.example.meditag.domain.chatbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenAiService {

    private final WebClient webClient;  // WebClient는 @Bean으로 주입됨

    public String sendMessageToOpenAi(String prompt) {
        return webClient.post()
                .uri("") // WebClient에서 baseUrl이 설정되어 있으므로 상대 URI만 지정
                .bodyValue(Map.of(
                        "model", "gpt-3.5-turbo",
                        "messages", List.of(
                                Map.of("role", "user", "content", prompt)
                        ),
                        "max_tokens", 100,
                        "temperature", 0.7
                ))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
