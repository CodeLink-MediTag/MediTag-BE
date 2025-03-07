package com.example.meditag.domain.auth.controller.api;

import com.example.meditag.domain.auth.dto.LoginDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;


@Tag(name = "인증 관리", description = "인증 관련 API")
public interface AuthApi {

    @Operation(summary = "일반 로그인", description = "일반 로그인을 수행하고 JWT 토큰을 반환합니다.\n" +
            "```json\n" +
            "{\n" +
            "  \"username\": \"test@gmail.com\",\n" +
            "  \"password\": \"test12345\"\n" +
            "}\n" +
            "```")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(schema = @Schema(type = "string", example = "Bearer eyJhbGciOiJIUzI1..."))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(type = "string", example = "아이디 또는 비밀번호가 일치하지 않습니다.")))
    })
    ResponseEntity<String> login(
            @Parameter(description = "회원 정보", required = true) LoginDTO loginDTO
    );
}