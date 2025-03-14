package com.example.meditag.domain.jwt.util;

import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component // 스프링에서 관리되는 빈으로 등록
public class JWTUtil {

    private SecretKey secretKey; // JWT 서명에 사용할 비밀 키

    public JWTUtil(@Value("${spring.jwt.secret}") String secret) {
        secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());
        log.info("[JWTUtil] JWT secretKey 생성: {}", secretKey);
    }

    // 토큰에서 username 추출
    public String getUsername(String token) {
        log.info("[JWTUtil/getUsername] 토큰에서 username 추출, 토큰: {}", token);  // 토큰 정보도 로그에 남김
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("username", String.class);
    }

    // 토큰에서 role 추출
    public String getRole(String token) {
        log.info("[JWTUtil/getRole] 토큰에서 role 추출, 토큰: {}", token);  // 토큰 정보도 로그에 남김
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }

    // 토큰에서 provider 추출
    public String getProvider(String token) {
        log.info("[JWTUtil/getProvider] 토큰에서 provider 추출, 토큰: {}", token);
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("provider", String.class);
    }

    // 토큰에서 providerId 추출
    public String getProviderId(String token) {
        log.info("[JWTUtil/getProvider] 토큰에서 provider 추출, 토큰: {}", token);
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("providerId", String.class);
    }

    // 토큰이 만료되었는지 확인
    public Boolean isExpired(String token) {
        log.info("[JWTUtil/isExpired] 토큰 만료 여부 확인, 토큰: {}", token);  // 토큰 정보도 로그에 남김
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration()
                .before(new Date());
    }

    // 새로운 JWT 생성 (username, role, 만료 시간 지정)
    public String createAccessToken(String username, String role, Long expiredMs) {
        log.info("[JWTUtil/createJwt] 새로운 JWT 생성, username: {}, role: {}, 만료 시간(ms): {}", username, role, expiredMs);
        return Jwts.builder()
                .claim("username", username) // username 클레임 추가
                .claim("role", role) // role 클레임 추가
                .issuedAt(new Date(System.currentTimeMillis())) // 발급 시간 설정
                .expiration(new Date(System.currentTimeMillis() + expiredMs)) // 만료 시간 설정
                .signWith(secretKey) // 서명 추가
                .compact(); // 토큰 생성 및 반환
    }

    // Refresh Token
    public String createRefreshToken(String username, Long expiredMs) {
        log.info("[JWTUtil/createRefreshToken] 새로운 리프레시 토큰 생성, username: {}, 만료 시간(ms): {}", username, expiredMs);
        return Jwts.builder()
                .claim("username", username) // 리프레시 토큰은 일반적으로 username만 저장
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiredMs)) // 리프레시 토큰 만료 시간
                .signWith(secretKey)
                .compact();
    }
}

