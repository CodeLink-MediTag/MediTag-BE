package com.example.meditag.domain.medicine.service;

import com.example.meditag.domain.medicine.dto.request.MedicineCreateRequestDTO;
import com.example.meditag.domain.medicine.dto.response.MedicineCreateResponseDTO;
import com.example.meditag.domain.medicine.entity.Medicine;
import com.example.meditag.domain.medicine.repository.MedicineRepository;
import com.example.meditag.domain.member.entity.Member;
import com.example.meditag.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MedicineService {

    private final MedicineRepository medicineRepository;
    private final MemberRepository memberRepository;

    public MedicineCreateResponseDTO saveMedicine(String username, MedicineCreateRequestDTO requestDto) {

        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        Medicine medicine = Medicine.builder()
                .name(requestDto.getName())
                .characteristic(requestDto.getCharacteristic())
                .startDate(requestDto.getStartDate())
                .duration(requestDto.getDuration())
                .frequency(requestDto.getFrequency())
                .imageUrl(requestDto.getImageUrl())
                .isPrescribed(requestDto.isPrescribed())
                .member(member)
                .build();

        Medicine save = medicineRepository.save(medicine);

        return MedicineCreateResponseDTO.builder()
                .id(save.getId())
                .name(save.getName())
                .characteristic(save.getCharacteristic())
                .startDate(save.getStartDate())
                .duration(save.getDuration())
                .frequency(save.getFrequency())
                .imageUrl(save.getImageUrl())
                .isPrescribed(save.isPrescribed())
                .build();
    }
}
