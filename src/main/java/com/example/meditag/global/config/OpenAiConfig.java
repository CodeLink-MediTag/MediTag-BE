package com.example.meditag.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OpenAiConfig {

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.project}")
    private String apiProject;

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl(apiUrl) // 기본 URL 설정
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey) // 인증 헤더 추가
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json") // Content-Type 추가
                .defaultHeader("OpenAI-Project", apiProject) // 프로젝트 ID 추가
                .build();
    }
}
