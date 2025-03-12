package com.example.meditag.global.config;

import com.example.meditag.domain.jwt.repository.RefreshTokenRedisRepository;
import com.example.meditag.domain.oauth2.hendler.CustomSuccessHandler;
import com.example.meditag.domain.oauth2.service.CustomOAuth2UserService;
import com.example.meditag.domain.jwt.filter.JWTFilter;
import com.example.meditag.global.jwt.JWTUtil;
import com.example.meditag.domain.auth.filter.LoginFilter;
import com.example.meditag.domain.auth.filter.CustomLogoutFilter;
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

    private final RefreshTokenRedisRepository refreshTokenRedisRepository;



    // 🔹 생성자를 통해 AuthenticationConfiguration과 JWTUtil을 주입받음
    public SecurityConfig(AuthenticationConfiguration authenticationConfiguration, JWTUtil jwtUtil, CustomOAuth2UserService customOAuth2UserService, CustomSuccessHandler customSuccessHandler, RefreshTokenRedisRepository refreshTokenRedisRepository) {
        this.authenticationConfiguration = authenticationConfiguration;
        this.jwtUtil = jwtUtil;
        this.customOAuth2UserService = customOAuth2UserService;
        this.customSuccessHandler = customSuccessHandler;
        this.refreshTokenRedisRepository = refreshTokenRedisRepository;
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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // CORS 설정
        http.cors(corsCustomizer -> corsCustomizer.configurationSource(new CorsConfigurationSource() {
            @Override
            public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(Collections.singletonList("http://localhost:3000"));
                configuration.setAllowedMethods(Collections.singletonList("*"));
                configuration.setAllowCredentials(true);
                configuration.setAllowedHeaders(Collections.singletonList("*"));
                configuration.setMaxAge(3600L);
                configuration.setExposedHeaders(Collections.singletonList("Authorization"));
                return configuration;
            }
        }));

        // CSRF 비활성화
        http.csrf(csrf -> csrf.disable());

        // 로그인 설정 (커스텀 로그인 페이지)
        http.formLogin(form -> form
                .loginPage("/login")  // 커스텀 로그인 페이지
                .permitAll()
        );

        // HTTP Basic 인증 비활성화
        http.httpBasic(httpBasic -> httpBasic.disable());

        // OAuth2 로그인 설정
        http.oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                .successHandler(customSuccessHandler)
        );

        // 경로별 권한 설정
        // SecurityConfig.java의 filterChain 메소드 내부 수정
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/api/auth/login",
                        "/",
                        "/api/member/register",
                        "/login/oauth2/code/naver",
                        "/login/oauth2/code/kakao",
                        "/login",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/api-docs/**",
                        "/swagger-resources/**",
                        "/api/jwt/reissue"
                )
                .permitAll()
                .requestMatchers("/admin").hasRole("ADMIN")
                .anyRequest().authenticated()
        );

        // JWTFilter를 LoginFilter 이전에 실행되도록 설정
        http.addFilterBefore(new JWTFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

        // LoginFilter를 JWTFilter 앞에 설정하여 로그인 후 토큰 검증이 가능하게 함
        http.addFilterAfter(new LoginFilter(authenticationManager(authenticationConfiguration), jwtUtil, refreshTokenRedisRepository), JWTFilter.class);

        // CustomLogoutFilter 등록
        http.addFilterBefore(new CustomLogoutFilter(jwtUtil, refreshTokenRedisRepository), UsernamePasswordAuthenticationFilter.class);

        // 세션을 사용하지 않도록 설정
        http.sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        return http.build();
    }
}
