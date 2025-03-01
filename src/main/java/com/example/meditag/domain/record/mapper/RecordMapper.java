package com.example.meditag.domain.record.mapper;

import com.example.meditag.domain.member.entity.Member;
import com.example.meditag.domain.record.dto.RecordingCreateRequestDTO;
import com.example.meditag.domain.record.dto.RecordingResponseDTO;
import com.example.meditag.domain.record.entity.Recording;

public class RecordMapper {
    //RecordingCreateRequestDTO -> Recording
    public static Recording toRecording(RecordingCreateRequestDTO requestDTO, Member member, String ImageUrl) {
        return Recording.builder()
                .title(requestDTO.getTitle())
                .recordingTime(requestDTO.getRecordingTime())
                .recordingFile(ImageUrl)
                .member(member)
                .build();

    }
    //Recording -> toResponseDTO
    public static RecordingResponseDTO toResponseDTO(Recording recording) {
        return RecordingResponseDTO.builder()
                .title(recording.getTitle())
                .recordingTime(recording.getRecordingTime())
                .recordingFile(recording.getRecordingFile())
                .build();

    }
}
