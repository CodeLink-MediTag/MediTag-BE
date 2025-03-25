package com.example.meditag.domain.chatbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenAiService {

    private final WebClient webClient; // OpenAiConfig에서 생성된 WebClient를 주입받음

    @Value("${openai.api.url}")
    private String apiUrl;

    public String sendMessageToOpenAi(String prompt) {

        return webClient.post()
                .uri(apiUrl)
                .bodyValue(Map.of(
                        "model", "gpt-3.5-turbo",
                        "messages", List.of(
                                Map.of("role", "user", "content", prompt)
                        ),
                        "max_tokens", 100,
                        "temperature", 0.7
                ))
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), clientResponse -> {
                    // 4xx 에러 로그 출력
                    System.err.println("4xx Client Error occurred!");
                    return clientResponse.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                System.err.println("Error Body: " + errorBody);
                                return Mono.error(new RuntimeException("4xx Error: " + errorBody));
                            });
                })
                .onStatus(status -> status.is5xxServerError(), clientResponse -> {
                    // 5xx 에러 로그 출력
                    System.err.println("5xx Server Error occurred!");
                    return Mono.error(new RuntimeException("5xx Server Error"));
                })
                .bodyToMono(String.class)
                .doOnNext(response -> {
                    // 성공 응답 로그 출력
                    System.out.println("Response received from OpenAI API:");
                    System.out.println(response);
                })
                .doOnError(error -> {
                    // 에러 로그 출력
                    System.err.println("An error occurred during API call:");
                    error.printStackTrace();
                })
                .block();
    }
}
