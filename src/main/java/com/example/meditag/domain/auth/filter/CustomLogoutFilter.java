package com.example.meditag.domain.auth.filter;

import com.example.meditag.domain.jwt.repository.RefreshTokenRedisRepository;
import com.example.meditag.domain.jwt.filter.JWTUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class CustomLogoutFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;
    private final RefreshTokenRedisRepository refreshTokenRedisRepository;

    public CustomLogoutFilter(JWTUtil jwtUtil, RefreshTokenRedisRepository refreshTokenRedisRepository) {
        this.jwtUtil = jwtUtil;
        this.refreshTokenRedisRepository = refreshTokenRedisRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 로그아웃 엔드포인트 확인
        if (!request.getRequestURI().equals("/api/auth/logout")) {
            filterChain.doFilter(request, response);
            return;
        }

        log.info("[CustomLogoutFilter] 로그아웃 처리 시작");

        try {
            // Authorization 헤더에서 토큰 추출
            String authorization = request.getHeader("Authorization");

            // 토큰이 없으면 로그아웃을 할 수 없으므로, 클라이언트에게 오류 응답을 반환
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400 Bad Request
                response.getWriter().write("토큰이 없습니다. 로그아웃을 진행할 수 없습니다.");
                return;
            }

            String token = authorization.split(" ")[1];

            // 토큰에서 사용자 정보 추출
            String username = jwtUtil.getUsername(token);

            // Redis에서 RefreshToken 삭제
            refreshTokenRedisRepository.deleteRefreshToken(username);
            log.info("[CustomLogoutFilter] Redis에서 RefreshToken 삭제 완료: {}", username);

            // 쿠키에서 RefreshToken 삭제
            Cookie cookie = new Cookie("refresh", null);
            cookie.setMaxAge(0);
            cookie.setPath("/");
            response.addCookie(cookie);

            // SecurityContext 초기화
            SecurityContextHolder.clearContext();

            log.info("[CustomLogoutFilter] 로그아웃 처리 완료: {}", username);

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("로그아웃 되었습니다.");
        } catch (Exception e) {
            log.error("[CustomLogoutFilter] 로그아웃 처리 중 오류 발생", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("로그아웃 처리 중 오류가 발생했습니다.");
        }
    }
}
