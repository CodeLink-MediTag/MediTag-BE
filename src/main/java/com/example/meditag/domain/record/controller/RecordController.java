package com.example.meditag.domain.record.controller;

import com.example.meditag.domain.record.dto.request.RecordRequestDTO;
import com.example.meditag.domain.record.dto.response.RecordResponseDTO;
import com.example.meditag.domain.record.service.RecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/records")
public class RecordController {

    private final RecordService recordService;

    //녹음 목록 조회 API
    @GetMapping
    public ResponseEntity<List<RecordResponseDTO>> getRecords() {
        List<RecordResponseDTO> records = recordService.getRecords();
        return ResponseEntity.ok(records);
    }

    //녹음 저장 API
    @PostMapping
    public ResponseEntity<String> createRecord(@RequestBody RecordRequestDTO recordRequestDTO) {
        RecordResponseDTO record = recordService.createRecord(recordRequestDTO);
        return ResponseEntity.ok("저장 성공!" + record.toString());
    }

    //녹음 삭제 API
    @DeleteMapping("/{recordId}")
    public ResponseEntity<String> deleteRecord(@PathVariable Long recordId) {
        recordService.deleteRecord(recordId);
        return ResponseEntity.ok("삭제 성공!");
    }
}
