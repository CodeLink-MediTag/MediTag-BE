package com.example.meditag.domain.member.controller.api;

import com.example.meditag.domain.auth.dto.CustomUserDetails;
import com.example.meditag.domain.member.dto.request.GuardianRequestDTO;
import com.example.meditag.domain.member.dto.response.GuardianResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Guardian API", description = "보호자 관리 API")
@RequestMapping("/api/guardians")
public interface GuardianApi {

    @Operation(summary = "보호자 등록", description = "새로운 보호자를 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "등록 성공",
                    content = @Content(schema = @Schema(implementation = GuardianResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping
    ResponseEntity<GuardianResponseDTO> registerGuardian(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @RequestBody @Parameter(description = "보호자 요청 DTO", required = true) GuardianRequestDTO requestDTO
    );

    @Operation(summary = "보호자 목록 조회", description = "회원의 모든 보호자 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GuardianResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping
    ResponseEntity<List<GuardianResponseDTO>> getGuardians(
            @Parameter(hidden = true) CustomUserDetails userDetails
    );

    @Operation(summary = "보호자 삭제", description = "특정 보호자를 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공",
                    content = @Content(schema = @Schema(example = "{\"message\": \"보호자가 성공적으로 삭제되었습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "보호자를 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @DeleteMapping("/{guardianId}")
    ResponseEntity<Map<String, String>> deleteGuardian(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @PathVariable @Parameter(description = "삭제할 보호자의 ID", required = true) Long guardianId
    );
}
