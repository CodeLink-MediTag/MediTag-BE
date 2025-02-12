package com.example.meditag.global.jwt;

import com.example.meditag.domain.auth.dto.request.CustomUserDetails;
import com.example.meditag.domain.auth.dto.request.LoginDTO;
import com.example.meditag.domain.auth.dto.response.TokenDTO;
import com.example.meditag.global.error.exception.CustomException;
import com.example.meditag.global.error.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

@Slf4j
public class LoginFilter extends UsernamePasswordAuthenticationFilter { // 로그인 시 동작하는 필터

    private final AuthenticationManager authenticationManager; // 인증을 담당하는 매니저
    private final JWTUtil jwtUtil; // JWT 유틸리티 클래스
//    private final CustomAuthenticationEntryPoint authenticationEntryPoint; // 예외 처리 담당

    public LoginFilter(AuthenticationManager authenticationManager, JWTUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
//        this.authenticationEntryPoint = authenticationEntryPoint;
        setFilterProcessesUrl("/api/auth/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        // 클라이언트 요청에서 username, password 추출
        String username = obtainUsername(request);
        String password = obtainPassword(request);

        // UsernamePasswordAuthenticationToken을 생성하여 인증 매니저로 전달
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password, null);

        // 인증 매니저를 통해 인증 수행
        return authenticationManager.authenticate(authToken);
    }

    // 로그인 성공 시 실행되는 메서드 (JWT 발급)
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException {
        // 인증된 사용자 정보 가져오기
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        String username = customUserDetails.getUsername();

        // 사용자 권한 가져오기
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();
        String role = auth.getAuthority(); // 역할(role) 가져오기

        log.info("Generating JWT token for username: {} with role: {}", username, role);

        // JWT 토큰 생성 (10시간 유효)
        String token = jwtUtil.createJwt(username, role, 60 * 60 * 10L);

        // TokenDTO 생성 및 JSON 응답
        TokenDTO tokenDTO = TokenDTO.builder()
                .accessToken(token)
                .build();

        // JSON 응답 설정 (반환값)
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        new ObjectMapper().writeValue(response.getWriter(), tokenDTO);

        // 응답 헤더에 JWT 추가
        response.addHeader("Authorization", "Bearer " + token);

        log.info("JWT token successfully generated and added to response header");
    }

    // 로그인 실패 시 실행되는 메서드 (CustomAuthenticationEntryPoint 활용)
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        log.error("Authentication failed: {}", failed.getMessage());

        response.setStatus(401);

        // CustomAuthenticationEntryPoint를 사용하여 예외 처리
//        authenticationEntryPoint.commence(request, response, failed);
    }
}
