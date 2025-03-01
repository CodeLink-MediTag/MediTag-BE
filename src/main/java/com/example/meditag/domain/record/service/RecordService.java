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
import java.util.UUID;

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
}
