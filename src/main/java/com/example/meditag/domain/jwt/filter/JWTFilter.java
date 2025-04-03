package com.example.meditag.domain.jwt.filter;

import com.example.meditag.domain.auth.dto.CustomUserDetails;
import com.example.meditag.domain.jwt.util.JWTUtil;
import com.example.meditag.domain.member.entity.Member;
import com.example.meditag.global.error.exception.CustomAuthenticationException;
import com.example.meditag.global.error.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JWTFilter extends OncePerRequestFilter { // 한 요청당 한 번만 실행되는 필터 클래스

    private final JWTUtil jwtUtil; // JWT 관련 유틸리티 클래스

    public JWTFilter(JWTUtil jwtUtil) { // 생성자에서 JWTUtil 객체를 주입받음
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.startsWith("/api/member/register")
                || path.startsWith("/api/auth/login")
                || path.startsWith("/api/auth/kakao-login")
                || path.startsWith("/api/jwt/reissue")
                || path.startsWith("/login")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/swagger-ui.html")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 요청 헤더에서 Authorization 값을 가져옴
        String authorization = request.getHeader("Authorization");

        // Authorization 헤더가 없거나 "Bearer "로 시작하지 않으면 필터 진행 후 종료
        if (authorization == null || !authorization.startsWith("Bearer ")) {
           throw new CustomAuthenticationException(ErrorCode.TOKEN_MISSING);
        }

        // "Bearer " 부분을 제거하고 순수한 토큰 값만 추출
        String token = authorization.split(" ")[1];

        // 토큰의 만료 여부 확인
        if (jwtUtil.isExpired(token)) {
            throw new CustomAuthenticationException(ErrorCode.TOKEN_EXPIRED);
        }

        // 토큰에서 username과 role을 추출
        String username = jwtUtil.getUsername(token);
        String role = jwtUtil.getRole(token);

        // Builder 패턴을 사용하여 Member 객체 생성
        Member member = Member.builder()
                .username(username)
                .role(role)
                .build();

        // UserEntity 정보를 기반으로 CustomUserDetails 객체 생성
        CustomUserDetails customUserDetails = new CustomUserDetails(member);

        // 스프링 시큐리티 인증 토큰 생성
        Authentication authToken = new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());

        // 시큐리티 컨텍스트에 인증 정보 설정
        SecurityContextHolder.getContext().setAuthentication(authToken);

        // 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }
}
