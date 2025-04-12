package com.example.meditag.domain.oauth2.service;

import com.example.meditag.domain.jwt.dto.TokenDTO;
import com.example.meditag.domain.jwt.repository.RefreshTokenRedisRepository;
import com.example.meditag.domain.jwt.util.JWTUtil;
import com.example.meditag.domain.member.entity.Member;
import com.example.meditag.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoLoginService {

    private final MemberRepository memberRepository;
    private final JWTUtil jwtUtil;
    private final RefreshTokenRedisRepository refreshTokenRedisRepository;

    public TokenDTO loginWithKakao(String kakaoAccessToken) {

        // 1. 카카오 API로 유저 정보 얻기
        RestTemplate rt = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + kakaoAccessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        HttpEntity<String> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = rt.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.GET,
                request,
                Map.class
        );

        Map<String, Object> attributes = response.getBody();
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        String username = "kakao " + attributes.get("id").toString();
        String email = (String) kakaoAccount.get("email");
        String nickname = (String) profile.get("nickname");

        // 2. DB에서 사용자 확인 및 저장
        Optional<Member> existData = memberRepository.findByUsername(username);
        Member member;

        if (existData.isEmpty()) {
            member = Member.builder()
                    .username(username)
                    .name(nickname)
                    .role("ROLE_USER")
                    .build();
            memberRepository.save(member);
            log.info("[KakaoLoginService] 신규 회원 가입 완료");
        } else {
            member = existData.get();
            log.info("[KakaoLoginService] 기존 회원 로그인 완료");
        }

        // 3. JWT 생성
        String access = jwtUtil.createAccessToken(member.getUsername(), member.getRole(), 3600_000L); // 1시간
        String refresh = jwtUtil.createRefreshToken(member.getUsername(), 30L * 24 * 3600 * 1000); // 30일

        refreshTokenRedisRepository.saveRefreshToken(member.getUsername(), refresh);
        log.info("[KakaoLoginService] JWT 발급 완료");

        return new TokenDTO(access, refresh);
    }
}