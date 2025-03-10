package com.example.meditag.global.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TokenDTO {

    private String accessToken;  // 액세스 토큰
    private String refreshToken; // 리프레시 토큰 (선택)
}
