package com.example.meditag.domain.record.service;

import com.example.meditag.domain.record.dto.request.RecordRequestDTO;
import com.example.meditag.domain.record.dto.response.RecordResponseDTO;
import com.example.meditag.domain.record.repository.RecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecordService {

    private final RecordRepository recordRepository;

    //녹음 목록 조회 API
    @Transactional
    public List<RecordResponseDTO> getRecords() {

    }

    //녹음 저장 API
    @Transactional
    public RecordResponseDTO createRecord(RecordRequestDTO recordRequestDTO) {

    }

    //녹음 삭제 API
    @Transactional
    public RecordResponseDTO deleteRecord(Long recordId) {

    }
}
