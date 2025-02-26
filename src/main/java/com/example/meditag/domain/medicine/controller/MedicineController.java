package com.example.meditag.domain.medicine.controller;

import com.example.meditag.domain.medicine.dto.request.MedicineRequestDto;
import com.example.meditag.domain.medicine.entity.Medicine;
import com.example.meditag.domain.medicine.repository.MedicineRepository;
import com.example.meditag.domain.medicine.service.MedicineService;
import com.example.meditag.global.aws.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/medicines")
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService medicineService;
    private final S3Service s3Service;
    //약 알림 등록
    @PostMapping("/reminders")
    public ResponseEntity<String> reminders(@RequestBody MedicineRequestDto requestDto) {


        medicineService.saveMedicine(requestDto);

        return ResponseEntity.ok("약이 성공적으로 저장되었습니다.");
    }



    @GetMapping("/presigned-url")
    @ResponseBody
    String getUrl(@RequestParam String filename){
        var result = s3Service.createPresignedUrl("test/"+filename);
        return result;
    }


}
