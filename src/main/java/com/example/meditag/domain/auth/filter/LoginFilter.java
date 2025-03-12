package com.example.meditag.domain.auth.filter;

import com.example.meditag.domain.auth.dto.CustomUserDetails;
import com.example.meditag.domain.auth.dto.LoginDTO;
import com.example.meditag.domain.jwt.repository.RefreshTokenRedisRepository;
import com.example.meditag.global.error.ErrorResponse;
import com.example.meditag.global.error.exception.ErrorCode;
import com.example.meditag.domain.jwt.filter.JWTUtil;
import com.example.meditag.domain.jwt.dto.TokenDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
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
    private final RefreshTokenRedisRepository refreshTokenRedisRepository;
//    private final CustomAuthenticationEntryPoint authenticationEntryPoint; // 예외 처리 담당

    public LoginFilter(AuthenticationManager authenticationManager, JWTUtil jwtUtil, RefreshTokenRedisRepository refreshTokenRedisRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.refreshTokenRedisRepository = refreshTokenRedisRepository;
//        this.authenticationEntryPoint = authenticationEntryPoint;
        setFilterProcessesUrl("/api/auth/login");
        log.info("[LoginFilter] LoginFilter 생성자 주입");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            // JSON 요청을 LoginDTO 객체로 변환
            LoginDTO loginDTO = new ObjectMapper().readValue(request.getInputStream(), LoginDTO.class);

            log.info("[LoginFilter/attemptAuthentication] 1. loginDTO로 객체 변환 email:{}, password:{}", loginDTO.getUsername(), loginDTO.getPassword());

            // UsernamePasswordAuthenticationToken 생성
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    loginDTO.getUsername(),
                    loginDTO.getPassword()
            );

            log.info("[LoginFilter/attemptAuthentication] 2. UsernamePasswordAuthenticationToken 생성 authToken: {}", authToken);

            // 인증 시도
            return authenticationManager.authenticate(authToken);

        } catch (IOException e) {
            log.error("[LoginFilter] 로그인 중에 에러", e);
            throw new AuthenticationServiceException("로그인 입력을 읽는 도중 오류 발생: " + e.getMessage());
        }
    }

    // 로그인 성공 시 실행되는 메서드 (JWT 발급)
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException {
        // 인증된 사용자 정보 가져오기
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        String username = customUserDetails.getUsername();

        log.info("[LoginFilter/successfulAuthentication] 3. 인증된 사용자 정보 가져오기: {}", username);

        // 사용자 권한 가져오기
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();
        String role = auth.getAuthority(); // 역할(role) 가져오기

        log.info("[LoginFilter/successfulAuthentication] 4. 인증된 사용자 권한 가져오기: {}", role);

        // JWT 토큰 생성
        String access = jwtUtil.createAccessToken(username, role, 60 * 60 * 1000L);
        String refresh = jwtUtil.createRefreshToken(username, 60 * 60 * 60 * 1000L);

        log.info("[LoginFilter/successfulAuthentication] 5. JWT 토큰 생성 - Access: {}, Refresh: {}", access, refresh);

        // RefreshToken 저장 (Redis)
        refreshTokenRedisRepository.saveRefreshToken(username, refresh);

        // TokenDTO 생성 및 JSON 응답
        TokenDTO tokenDTO = TokenDTO.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .build();

        log.info("[LoginFilter/successfulAuthentication] 6. JWT TokenDTO 생성: {}", tokenDTO);

        // 응답 헤더 설정
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpStatus.OK.value());
        response.addHeader("Authorization", "Bearer " + access);

        // 리프레시 토큰을 쿠키에 추가
        Cookie refreshTokenCookie = createCookie("refresh", refresh);

        // JSON 응답 반환
        new ObjectMapper().writeValue(response.getWriter(), tokenDTO);

        log.info("[LoginFilter/successfulAuthentication] 7. JWT 토큰 HTTP 헤더 및 쿠키 설정 완료");
    }

    private Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24 * 60 * 60); // 1일 동안 유지
        cookie.setPath("/"); // 모든 경로에서 접근 가능하도록 설정
        cookie.setHttpOnly(true); // JavaScript에서 접근 불가능하도록 설정 (보안 강화)

        // HTTPS 환경이 아니라면 secure 설정 주석 처리
        // cookie.setSecure(true);

        return cookie;
    }


    // 로그인 실패 시 실행되는 메서드
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {
        log.error("Authentication failed: {}", failed.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ErrorCode errorCode = ErrorCode.AUTHENTICATION_FAILED; // 기본적으로 로그인 실패 오류 사용

        // 오류 응답 생성
        ErrorResponse errorResponse = new ErrorResponse(errorCode.getStatus().value(), errorCode.getMessage());

        // JSON 응답 반환
        new ObjectMapper().writeValue(response.getWriter(), errorResponse);
    }
}
