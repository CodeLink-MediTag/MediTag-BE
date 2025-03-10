package com.example.meditag.domain.oauth2.hendler;

import com.example.meditag.global.jwt.TokenDTO;
import com.example.meditag.domain.oauth2.dto.CustomOAuth2User;
import com.example.meditag.global.jwt.JWTUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

@Slf4j
@Component
public class CustomSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JWTUtil jwtUtil;

    public CustomSuccessHandler(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
        log.info("[CustomSuccessHandler] CustomSuccessHandler 생성자 주입 완료");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("[CustomSuccessHandler/onAuthenticationSuccess] 인증 성공 처리 시작");

        // CustomOAuth2User 정보 추출
        CustomOAuth2User customOAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        log.info("[CustomSuccessHandler/onAuthenticationSuccess] 1. customOAuth2User 정보: {}", customOAuth2User);

        String username = customOAuth2User.getUsername();
        log.info("[CustomSuccessHandler/onAuthenticationSuccess] 2. 인증된 사용자: {}", username);

        // 권한 정보 추출
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next(); // 첫 번째 권한만 가져오기
        String role = auth.getAuthority();
        log.info("[CustomSuccessHandler/onAuthenticationSuccess] 3. 사용자 권한: {}", role);

        // JWT 생성
        String access = jwtUtil.createJwt(username, role, 60 * 60 * 60L);
        String refresh = jwtUtil.createJwt(username, role, 60 * 60 * 60 * 24L);
        log.info("[CustomSuccessHandler/onAuthenticationSuccess] 4. JWT 생성 완료, 토큰: {}", access);

        // 응답 헤더 및 JSON 응답 추가
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        new ObjectMapper().writeValue(response.getWriter(), new TokenDTO(access, refresh));
        log.info("[CustomSuccessHandler/onAuthenticationSuccess] 5. JWT 응답 전송 완료");

        // JWT를 Authorization 헤더에 추가
        response.addHeader("Authorization", "Bearer " + access);
        response.addCookie(createCookie("refresh", refresh));
        response.setStatus(HttpStatus.OK.value());
        log.info("[CustomSuccessHandler/onAuthenticationSuccess] 6. Authorization 헤더에 JWT 추가 완료");

        log.info("[CustomSuccessHandler/onAuthenticationSuccess] 인증 성공 처리 종료");
    }

    private Cookie createCookie(String key, String value) {

        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24*60*60);
        //cookie.setSecure(true);
        //cookie.setPath("/");
        cookie.setHttpOnly(true);

        return cookie;
    }
}
