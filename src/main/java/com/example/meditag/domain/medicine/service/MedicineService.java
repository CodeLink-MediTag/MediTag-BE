package com.example.meditag.domain.medicine.service;

import com.example.meditag.domain.medicine.dto.request.MedicineRequestDto;
import com.example.meditag.domain.medicine.entity.Medicine;
import com.example.meditag.domain.medicine.mapper.MedicineMapper;
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

    public void saveMedicine(MedicineRequestDto requestDto) {

        Member member =memberRepository.findById(requestDto.getMemberId())
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

        medicineRepository.save(medicine);


    }

}
