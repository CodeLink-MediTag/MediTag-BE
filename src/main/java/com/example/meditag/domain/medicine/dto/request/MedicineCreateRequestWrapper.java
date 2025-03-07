package com.example.meditag.domain.medicine.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

@Schema(description = "약 정보 등록 요청")
public class MedicineCreateRequestWrapper {

    @Schema(description = "약 정보", required = true)
    private MedicineCreateRequestDTO data;

    @Schema(description = "약 이미지", type = "string", format = "binary")
    private MultipartFile file;
}