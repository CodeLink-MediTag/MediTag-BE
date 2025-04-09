package com.example.meditag.domain.auth.filter;

import com.example.meditag.domain.auth.dto.CustomUserDetails;
import com.example.meditag.domain.auth.dto.LoginDTO;
import com.example.meditag.domain.jwt.repository.RefreshTokenRedisRepository;
import com.example.meditag.domain.jwt.util.JWTUtil;
import com.example.meditag.domain.jwt.dto.TokenDTO;
import com.example.meditag.global.error.ErrorResponse;
import com.example.meditag.global.error.exception.CustomAuthenticationException;
import com.example.meditag.global.error.exception.ErrorCode;
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
public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;
    private final RefreshTokenRedisRepository refreshTokenRedisRepository;

    public LoginFilter(AuthenticationManager authenticationManager, JWTUtil jwtUtil, RefreshTokenRedisRepository refreshTokenRedisRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.refreshTokenRedisRepository = refreshTokenRedisRepository;
        setFilterProcessesUrl("/api/auth/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            LoginDTO loginDTO = new ObjectMapper().readValue(request.getInputStream(), LoginDTO.class);

            if (loginDTO.getUsername() == null || loginDTO.getUsername().isBlank() ||
                    loginDTO.getPassword() == null || loginDTO.getPassword().isBlank()) {
                throw new CustomAuthenticationException(ErrorCode.USERNAME_OR_PASSWORD_MISSING);
            }

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    loginDTO.getUsername(),
                    loginDTO.getPassword()
            );

            return authenticationManager.authenticate(authToken);

        } catch (CustomAuthenticationException e) {
            throw e;
        } catch (IOException e) {
            throw new AuthenticationServiceException("로그인 입력을 읽는 도중 오류 발생: " + e.getMessage());
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException {
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        String username = customUserDetails.getUsername();

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String role = authorities.iterator().next().getAuthority();

        String access = jwtUtil.createAccessToken(username, role, 60 * 60 * 1000L);
        String refresh = jwtUtil.createRefreshToken(username, 60 * 60 * 60 * 1000L);

        refreshTokenRedisRepository.saveRefreshToken(username, refresh);

        TokenDTO tokenDTO = TokenDTO.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .build();

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpStatus.OK.value());
        response.addHeader("Authorization", "Bearer " + access);

        Cookie refreshTokenCookie = createCookie("refresh", refresh);
        response.addCookie(refreshTokenCookie);

        new ObjectMapper().writeValue(response.getWriter(), tokenDTO);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {
        log.error("[LoginFilter] 로그인 실패: {}", failed.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ErrorCode errorCode;

        if (failed instanceof CustomAuthenticationException ex) {
            errorCode = ex.getErrorCode();
        } else {
            errorCode = ErrorCode.INVALID_CREDENTIALS;
        }

        ErrorResponse errorResponse = new ErrorResponse(errorCode.getStatus().value(), errorCode.getMessage());
        new ObjectMapper().writeValue(response.getWriter(), errorResponse);
    }

    private Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24 * 60 * 60);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        return cookie;
    }
}
