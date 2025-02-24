package com.example.meditag.domain.medicine.controller;

import com.example.meditag.domain.medicine.dto.request.MedicineRequestDto;
import com.example.meditag.domain.medicine.entity.Medicine;
import com.example.meditag.domain.medicine.repository.MedicineRepository;
import com.example.meditag.domain.medicine.service.MedicineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/medicines")
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService medicineService;
    //약 알림 등록
    @PostMapping("/reminders")
    public ResponseEntity<String> reminders(@RequestBody MedicineRequestDto requestDto) {
        medicineService.saveMedicine(requestDto);

        return ResponseEntity.ok("약이 성공적으로 저장되었습니다.");
    }


}
