package com.example.meditag.domain.member.controller.api;

import com.example.meditag.domain.member.dto.request.RegisterDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@Tag(name = "회원 관리", description = "회원 관련 API")
public interface MemberApi {

    @Operation(summary = "회원가입", description = "일반 회원가입을 합니다.\n" +
            "```json\n" +
            "{\n" +
            "  \"username\": \"test@gmail.com\",\n" +
            "  \"name\": \"회원1\",\n" +
            "  \"phone\": \"010-1234-5678\",\n" +
            "  \"password\": \"test12345\"\n" +
            "}\n" +
            "```")

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공",
                    content = @Content(schema = @Schema(implementation = RegisterDTO.class))),
            @ApiResponse(responseCode = "404", description = "회원가입 실패",
                    content = @Content(schema = @Schema(type = "string", example = "회원가입을 실패했습니다.")))
    })
    ResponseEntity<Map<String, RegisterDTO>> register(
            @Parameter(description = "회원 정보", required = true) RegisterDTO registerDTO
    );
}
