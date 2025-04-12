package com.example.meditag.domain.oauth2.controller.api;

import com.example.meditag.domain.jwt.dto.TokenDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "소셜 로그인", description = "카카오 소셜 로그인 API")
public interface OAuth2Api {

    @Operation(
            summary = "카카오 로그인 처리",
            description = "Flutter 앱에서 전달된 카카오 accessToken을 기반으로 회원가입 또는 로그인 처리 후 JWT를 발급합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "JWT 토큰 발급 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    ResponseEntity<TokenDTO> kakaoLogin(@RequestBody KakaoTokenRequest request);

    @Schema(description = "카카오 access token 요청 객체")
    class KakaoTokenRequest {
        @Schema(description = "카카오 access token", example = "eyJhbGciOiJIUzI1...")
        private String accessToken;

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }
    }
}