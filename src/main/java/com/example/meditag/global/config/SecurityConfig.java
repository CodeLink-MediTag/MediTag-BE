package com.example.meditag.global.config;

import com.example.meditag.domain.auth.service.LoginService;
import com.example.meditag.domain.oauth2.dto.CustomOAuth2User;
import com.example.meditag.domain.oauth2.hendler.CustomSuccessHandler;
import com.example.meditag.domain.oauth2.service.CustomOAuth2UserService;
import com.example.meditag.global.jwt.JWTFilter;
import com.example.meditag.global.jwt.JWTUtil;
import com.example.meditag.global.jwt.LoginFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Collections;

// 🔹 Spring Security 설정을 위한 클래스임을 나타내는 어노테이션
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 🔹 인증 관리를 위한 AuthenticationConfiguration 객체 (생성자 주입)
    private final AuthenticationConfiguration authenticationConfiguration;

    // 🔹 JWT 관련 유틸 클래스 (토큰 생성 및 검증)
    private final JWTUtil jwtUtil;

    private final CustomOAuth2UserService customOAuth2UserService;

    private final CustomSuccessHandler customSuccessHandler;



    // 🔹 생성자를 통해 AuthenticationConfiguration과 JWTUtil을 주입받음
    public SecurityConfig(AuthenticationConfiguration authenticationConfiguration, JWTUtil jwtUtil, CustomOAuth2UserService customOAuth2UserService, CustomSuccessHandler customSuccessHandler) {
        this.authenticationConfiguration = authenticationConfiguration;
        this.jwtUtil = jwtUtil;
        this.customOAuth2UserService = customOAuth2UserService;
        this.customSuccessHandler = customSuccessHandler;
    }

    // 🔹 AuthenticationManager를 Bean으로 등록
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // 🔹 비밀번호 암호화를 위한 BCryptPasswordEncoder Bean 등록
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 🔹 Spring Security의 필터 체인을 설정하는 메서드
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // 🔹 CORS 설정 (프론트엔드와의 통신 허용)
        http.cors((corsCustomizer -> corsCustomizer.configurationSource(new CorsConfigurationSource() {
            @Override
            public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
                CorsConfiguration configuration = new CorsConfiguration();

                // 허용할 도메인 설정 (React 프론트엔드에서 요청 허용)
                configuration.setAllowedOrigins(Collections.singletonList("http://localhost:3000"));

                // 모든 HTTP 메서드 허용 (GET, POST, PUT, DELETE 등)
                configuration.setAllowedMethods(Collections.singletonList("*"));

                // 인증 정보를 요청과 함께 전달할 수 있도록 설정
                configuration.setAllowCredentials(true);

                // 모든 헤더 허용
                configuration.setAllowedHeaders(Collections.singletonList("*"));

                // CORS 설정을 캐싱하는 시간 (초 단위)
                configuration.setMaxAge(3600L);

                // 클라이언트가 확인할 수 있도록 Authorization 헤더를 노출
                configuration.setExposedHeaders(Collections.singletonList("Authorization"));

                return configuration;
            }
        })));

        // 🔹 CSRF 보호 비활성화 (JWT 방식에서는 CSRF 필요 없음)
        http.csrf((auth) -> auth.disable());

        // 🔹 Form 로그인 방식 비활성화 (Spring Security 기본 로그인 폼 제거)
        http.formLogin((auth) -> auth.disable());

        // 🔹 HTTP Basic 인증 비활성화 (기본 인증 방식 제거)
        http.httpBasic((auth) -> auth.disable());


        //oauth2 로그인 설정
        http.oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                        .userService(customOAuth2UserService))
                        .successHandler(customSuccessHandler)

//                .successHandler((request, response, authentication) -> {
//                    // OAuth2 로그인 성공 시 JWT 토큰 발급
//                    CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
//                    String username = oAuth2User.getUsername();
//                    String role = oAuth2User.getAuthorities().stream()
//                            .findFirst()
//                            .map(GrantedAuthority::getAuthority)
//                            .orElse("ROLE_USER");
//
//                    String token = jwtUtil.createJwt(username, role, 60 * 60 * 10L);
//                    response.addHeader("Authorization", "Bearer " + token);
//                    response.sendRedirect("/"); // 로그인 성공 후 리다이렉트
//                })
        );


        // 🔹 경로별 접근 권한 설정
        http.authorizeHttpRequests((auth) -> auth
                .requestMatchers("/api/auth/login", "/", "/api/member/register").permitAll()  // 로그인, 회원가입, 홈 페이지는 인증 없이 접근 가능
                .requestMatchers("/admin").hasRole("ADMIN")           // '/admin' 경로는 ADMIN 역할이 있어야 접근 가능
                .anyRequest().authenticated()                         // 그 외의 모든 요청은 인증 필요
        );

        // 🔹 JWTFilter를 LoginFilter 이전에 실행되도록 등록
        http.addFilterBefore(new JWTFilter(jwtUtil), LoginFilter.class);

        // 🔹 로그인 요청을 처리하는 LoginFilter 등록
        //    LoginFilter는 AuthenticationManager와 JWTUtil을 필요로 함
        http.addFilterAt(new LoginFilter(authenticationManager(authenticationConfiguration), jwtUtil), UsernamePasswordAuthenticationFilter.class);

        // 🔹 세션을 사용하지 않도록 설정 (JWT 방식에서는 STATELESS 모드 사용)
        http.sessionManagement((session) -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 🔹 설정이 완료된 HttpSecurity 객체를 반환하여 Spring Security에 적용
        return http.build();
    }
}
