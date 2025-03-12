package com.example.meditag.domain.jwt.controller.api;

import com.example.meditag.domain.jwt.dto.TokenDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "JWT 관리", description = "JWT 관련 API")
public interface JWTApi {

    @Operation(summary = "토큰 재발급", description = "엑세스 토큰 만료 시 엑세스 토큰과 리프레시 토큰을 재발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(schema = @Schema(type = "string", example = "{\n" +
                            "  \"accessToken\": \"eyJhbGciOiJIUzI1NiJ9........\",\n" +
                            "  \"refreshToken\": \"eyJhbGciOiJIUzI1NiJ9.......\"\n" +
                            "}"))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(type = "string", example = "아이디 또는 비밀번호가 일치하지 않습니다.")))
    })
    ResponseEntity<TokenDTO> reissue(
            HttpServletRequest request, HttpServletResponse response
    );
}
