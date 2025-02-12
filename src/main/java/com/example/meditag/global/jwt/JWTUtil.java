package com.example.meditag.global.jwt;

import com.example.meditag.domain.member.entity.Member;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component // 스프링에서 관리되는 빈으로 등록
public class JWTUtil {

    private SecretKey secretKey; // JWT 서명에 사용할 비밀 키

    public JWTUtil(@Value("${spring.jwt.secret}") String secret) {
        // 설정 파일에서 가져온 비밀 키를 바이트 배열로 변환하여 SecretKey 객체 생성
        secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());
    }

    // 토큰에서 username 추출
    public String getUsername(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("username", String.class);
    }

    // 토큰에서 role 추출
    public String getRole(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }

    // 토큰이 만료되었는지 확인
    public Boolean isExpired(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration()
                .before(new Date());
    }

    // 새로운 JWT 생성 (username, role, 만료 시간 지정)
    public String createJwt(String username, String role, Long expiredMs) {
        return Jwts.builder()
                .claim("username", username) // username 클레임 추가
                .claim("role", role) // role 클레임 추가
                .issuedAt(new Date(System.currentTimeMillis())) // 발급 시간 설정
                .expiration(new Date(System.currentTimeMillis() + expiredMs)) // 만료 시간 설정
                .signWith(secretKey) // 서명 추가
                .compact(); // 토큰 생성 및 반환
    }
}
