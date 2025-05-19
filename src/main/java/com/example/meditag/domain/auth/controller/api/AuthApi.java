package com.example.meditag.domain.auth.controller.api;

import com.example.meditag.domain.auth.dto.LoginDTO;
import com.example.meditag.domain.jwt.dto.TokenDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
                    content = @Content(schema = @Schema(type = "string", example = "{\n" +
                            "  \"accessToken\": \"eyJhbGciOiJIUzI1NiJ9........\",\n" +
                            "  \"refreshToken\": \"eyJhbGciOiJIUzI1NiJ9.......\"\n" +
                            "}"))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(type = "string", example = "아이디 또는 비밀번호가 일치하지 않습니다.")))
    })
    ResponseEntity<String> login(
            @Parameter(description = "회원 정보", required = true) LoginDTO loginDTO
    );

    @Operation(summary = "로그아웃", description = "로그아웃을 수행하고 JWT 토큰을 삭제합니다(accessToken 넣어야함).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공",
                    content = @Content(schema = @Schema(type = "string", example = "로그아웃 되었습니다."))),
            @ApiResponse(responseCode = "400", description = "토큰이 없거나 유효하지 않음",
                    content = @Content(schema = @Schema(type = "string", example = "토큰이 없습니다. 로그아웃을 진행할 수 없습니다.")))
    })
    ResponseEntity<String> logout(
            @Parameter(description = "HTTP 요청", required = true) HttpServletRequest request
    );
}
