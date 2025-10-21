package com.example.meditag.domain.record.controller.api;

import com.example.meditag.domain.auth.dto.CustomUserDetails;
import com.example.meditag.domain.record.dto.RecordingCreateRequestDTO;
import com.example.meditag.domain.record.dto.RecordingResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "녹음 관리", description = "녹음 관련 API")
public interface RecordApi {

    @Operation(summary = "녹음 파일 저장", description = "녹음 파일을 저장합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "저장 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    ResponseEntity<String> createRecord(
            @Parameter(description = "사용자 정보") @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "녹음 정보", required = true) @RequestPart(value = "data") RecordingCreateRequestDTO requestDto,
            @Parameter(description = "녹음 파일") @RequestPart(value = "file", required = false) MultipartFile file
    );

    @Operation(summary = "녹음 파일 목록 조회", description = "모든 녹음 파일을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<List<RecordingResponseDTO>> getAllRecordings(
            @Parameter(description = "사용자 정보") @AuthenticationPrincipal CustomUserDetails customUserDetails
    );

    @Operation(
            summary = "녹음 파일 삭제",
            description = "녹음 ID로 해당 사용자의 녹음 파일과 S3 객체를 함께 삭제합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "해당 녹음 또는 사용자 정보를 찾을 수 없음"),
            @ApiResponse(responseCode = "403", description = "삭제 권한 없음(본인 소유 아님)")
    })
    ResponseEntity<String> deleteRecording(
            @Parameter(description = "사용자 정보") @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "삭제할 녹음 ID", schema = @Schema(example = "123")) @PathVariable("recordingId") Long recordingId
    );
}
