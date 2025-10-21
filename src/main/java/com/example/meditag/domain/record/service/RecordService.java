package com.example.meditag.domain.record.service;

import com.example.meditag.domain.member.entity.Member;
import com.example.meditag.domain.member.repository.MemberRepository;
import com.example.meditag.domain.record.dto.RecordingCreateRequestDTO;
import com.example.meditag.domain.record.dto.RecordingResponseDTO;
import com.example.meditag.domain.record.mapper.RecordMapper;
import com.example.meditag.domain.record.repository.RecordRepository;
import com.example.meditag.global.aws.S3Service;
import com.example.meditag.global.error.exception.CustomException;
import com.example.meditag.global.error.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.example.meditag.domain.record.entity.Recording;


import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordService {

    private final RecordRepository recordRepository;
    private final MemberRepository memberRepository;
    private final S3Service s3Service;

    @Transactional
    public RecordingResponseDTO createRecord(String username, RecordingCreateRequestDTO requstDTO, MultipartFile file ) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        //이미지 S3경로 가져오기
        String recordUrl = uploadRecordIfPresent(file);
        //엔티티 생성
        Recording recording = RecordMapper.toRecording(requstDTO, member, recordUrl);
        //저장
        Recording recordingSave = recordRepository.save(recording);


        return RecordMapper.toResponseDTO(recordingSave);
    }
    //S3업로드 밑 presigendUrl 반환
    private String uploadRecordIfPresent(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            String presignedUrl = s3Service.createPresignedUrl("recordings/" + fileName);

            // S3에 직접 업로드
            URL url = new URL(presignedUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", file.getContentType());

            try (OutputStream os = connection.getOutputStream()) {
                os.write(file.getBytes());
            }

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                // URL에서 query parameter 제거하여 실제 접근 URL만 반환
                return presignedUrl.split("\\?")[0];
            } else {
                throw new RuntimeException("파일 업로드 실패: HTTP 상태 코드 " + connection.getResponseCode());
            }
        } catch (IOException e) {
            throw new RuntimeException("파일 업로드 중 오류 발생", e);
        }
    }
    public List<RecordingResponseDTO> getAllRecordings(String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        List<Recording> recordings = recordRepository.findByMemberOrderByRecordingTimeDesc(member);

        return recordings.stream()
                .map(RecordMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteRecording(String username, Long recordingId) {
        var member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        var recording = recordRepository.findByIdAndMember(recordingId, member)
                .orElseThrow(() -> new CustomException(ErrorCode.RECORDING_NOT_FOUND)); // 필요시 RECORDING_NOT_FOUND로 분리

        // 1) S3 객체 삭제 (URL 기반)
        var fileUrl = recording.getRecordingFile();
        try {
            if (fileUrl != null && !fileUrl.isBlank()) {
                s3Service.deleteByUrl(fileUrl); // ✅ URL로 삭제
            }
        } catch (Exception e) {
            log.warn("S3 삭제 실패 recordingId={} url={} err={}", recordingId, fileUrl, e.getMessage());
            // 정책 선택: 실패 시에도 DB 삭제를 진행할지, 롤백할지 결정
            // throw new CustomException(ErrorCode.S3_DELETE_FAILED); // 롤백 원하면 사용
        }

        // 2) DB 삭제
        recordRepository.delete(recording);
    }
}
