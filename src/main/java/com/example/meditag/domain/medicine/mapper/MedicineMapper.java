package com.example.meditag.domain.medicine.mapper;

import com.example.meditag.domain.medicine.dto.request.MedicineCreateRequestDTO;
import com.example.meditag.domain.medicine.dto.response.MedicineResponseDTO;
import com.example.meditag.domain.medicine.entity.Medicine;
import com.example.meditag.domain.member.entity.Member;

public class MedicineMapper {

    // MedicineCreateReqeustDTO -> Medicine
    public static Medicine toMedicine(MedicineCreateRequestDTO medicineCreateRequestDTO, Member member, String imageUrl) {
        return Medicine.builder()
                .name(medicineCreateRequestDTO.getName())
                .characteristic(medicineCreateRequestDTO.getCharacteristic())
                .startDate(medicineCreateRequestDTO.getStartDate())
                .duration(medicineCreateRequestDTO.getDuration())
                .frequency(medicineCreateRequestDTO.getFrequency())
                .imageUrl(imageUrl)
                .prescribed(medicineCreateRequestDTO.isPrescribed())
                .member(member)
                .build();
    }

    // Medicine -> MedicineCreateResponseDTO
    public static MedicineResponseDTO toMedicineResponseDTO(Medicine medicine) {
        return MedicineResponseDTO.builder()
                .id(medicine.getId())
                .name(medicine.getName())
                .characteristic(medicine.getCharacteristic())
                .startDate(medicine.getStartDate())
                .duration(medicine.getDuration())
                .frequency(medicine.getFrequency())
                .imageUrl(medicine.getImageUrl())
                .isPrescribed(medicine.getPrescribed())
                .build();
    }
}
