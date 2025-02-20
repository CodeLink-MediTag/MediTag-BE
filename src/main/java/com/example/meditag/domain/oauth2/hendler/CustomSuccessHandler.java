package com.example.meditag.domain.oauth2.hendler;

import com.example.meditag.domain.oauth2.dto.CustomOAuth2User;
import com.example.meditag.global.jwt.JWTUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

@Component
public class CustomSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JWTUtil jwtUtil;

    public CustomSuccessHandler(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        CustomOAuth2User customOAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        String username = customOAuth2User.getUsername();

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority > iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();
        String role = auth.getAuthority();// 권한 작성

        String token = jwtUtil.createJwt(username, role, 60*60*60L);

        response.addCookie(createCookie("Authorization", token));//쿠기 넣어주기(네임,value)
        response.sendRedirect("http://localhost:8080/");// 로그인 성공 리다이렉트
    }
    //쿠키 생성 함수
    private Cookie createCookie( String key, String value){
        Cookie cookie = new Cookie(key, value);
        cookie.setPath("/");//전역에 쿠키를 보여줌
        cookie.setHttpOnly(true);// 자바스크립트가 가져가지 못하게 (쿠키)
        return cookie;
    }
}
