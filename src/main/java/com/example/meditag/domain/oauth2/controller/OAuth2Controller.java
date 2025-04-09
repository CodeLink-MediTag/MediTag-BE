package com.example.meditag.domain.oauth2.controller;

import com.example.meditag.domain.jwt.dto.TokenDTO;
import com.example.meditag.domain.oauth2.controller.api.OAuth2Api;
import com.example.meditag.domain.oauth2.controller.api.OAuth2Api.KakaoTokenRequest;
import com.example.meditag.domain.oauth2.service.KakaoLoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class OAuth2Controller implements OAuth2Api {

    private final KakaoLoginService kakaoLoginService;

    @Override
    @PostMapping("/kakao-login")
    public ResponseEntity<TokenDTO> kakaoLogin(@RequestBody KakaoTokenRequest request) {
        TokenDTO tokens = kakaoLoginService.loginWithKakao(request.getAccessToken());
        return ResponseEntity.ok(tokens);
    }
}
