package com.springweb.oauthsession.config;

import com.springweb.oauthsession.service.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final CustomOAuth2UserService customOAuth2UserService;

    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService) {
        this.customOAuth2UserService = customOAuth2UserService;
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf((csrf)-> csrf.disable());
        http
                .formLogin((login)-> login.disable());
        http
                .httpBasic((httpBasic)-> httpBasic.disable());
        http
                .oauth2Login((outh2) -> outh2
                        .userInfoEndpoint(userInfoEndpointConfig ->
                                userInfoEndpointConfig.userService(customOAuth2UserService)));
        http//인가작업
                .authorizeHttpRequests((auth) -> auth
                        .requestMatchers("/","/oauth2/**", "/login**", "/logout**").permitAll()//허용 루트
                        .anyRequest().authenticated());

        return http.build();
    }
}
