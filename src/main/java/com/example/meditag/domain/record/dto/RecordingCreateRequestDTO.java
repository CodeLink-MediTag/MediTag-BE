package com.example.meditag.domain.record.dto;

import com.example.meditag.domain.member.entity.Member;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RecordingCreateRequestDTO {
    private Long id;

    //제목
    private String title;

    //녹음 시간
    private LocalDateTime recordingTime;

    //녹음 파일경로
    private String recordingFile;

}
