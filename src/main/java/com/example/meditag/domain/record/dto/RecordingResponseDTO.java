package com.example.meditag.domain.record.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RecordingResponseDTO {

    private Long id;
    //제목
    private String title;

    //녹음 시간
    private LocalDateTime recordingTime;

    //녹음 파일경로
    private String recordingFile;
}
