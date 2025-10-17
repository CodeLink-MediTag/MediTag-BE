package com.example.meditag.global.config;

import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OpenAiConfig {

    private final Environment env;

    public OpenAiConfig(Environment env) {
        this.env = env;
    }

    @Bean
    public WebClient webClient() {
        String apiUrl = env.getProperty("openai.api.url");
        String apiKey = env.getProperty("openai.api.key");

        // 실제 API 키 로그로 출력하여 확인
        System.out.println("==== apiUrl 확인: [" + apiUrl + "]");
        System.out.println("==== apiKey 확인: [" + apiKey + "]");

        return WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                // 응답이 클 때 디코딩 오류 방지 (선택)
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                        .build())
                .build();
    }

}
