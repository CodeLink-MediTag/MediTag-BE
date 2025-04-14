package com.example.meditag.domain.chatbot.dto;

import com.example.meditag.domain.medicine.dto.response.MedicineGetDateResponseDTO;
import com.example.meditag.domain.record.entity.Recording;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DailyUserStatus {
    private String date;
    private List<MedicineGetDateResponseDTO.MedicineDTO> medicines;
    private List<Recording> recordings;
}
