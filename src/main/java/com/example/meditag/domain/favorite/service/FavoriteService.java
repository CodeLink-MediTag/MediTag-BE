package com.example.meditag.domain.favorite.service;

import com.example.meditag.domain.favorite.dto.FavoriteResponseDTO;
import com.example.meditag.domain.favorite.entity.Favorite;
import com.example.meditag.domain.favorite.repository.FavoriteRepository;
import com.example.meditag.domain.medicine.entity.Medicine;
import com.example.meditag.domain.medicine.repository.MedicineRepository;
import com.example.meditag.domain.member.entity.Member;
import com.example.meditag.domain.member.repository.MemberRepository;
import com.example.meditag.global.error.exception.CustomException;
import com.example.meditag.global.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final MedicineRepository medicineRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public FavoriteResponseDTO setFavorite(String username, Long medicineId) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        Medicine medicine = medicineRepository.findByIdAndMember(medicineId, member)
                .orElseThrow(() -> new CustomException(ErrorCode.MEDICINE_NOT_FOUND));

        favoriteRepository.deleteByMember(member);

        Favorite saved = favoriteRepository.save(
                Favorite.builder().member(member).medicine(medicine).build()
        );

        return FavoriteResponseDTO.builder()
                .medicineId(saved.getMedicine().getId())
                .medicineName(saved.getMedicine().getName())
                .characteristic(saved.getMedicine().getCharacteristic())
                .imageUrl(saved.getMedicine().getImageUrl())
                .prescribed(saved.getMedicine().getPrescribed())
                .build();
    }

    @Transactional
    public void removeFavorite(String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        favoriteRepository.deleteByMember(member);
    }

    @Transactional(readOnly = true)
    public FavoriteResponseDTO getFavorite(String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Favorite f = favoriteRepository.findByMember(member)
                .orElseThrow(() -> new CustomException(ErrorCode.FAVORITE_NOT_FOUND));

        return FavoriteResponseDTO.builder()
                .medicineId(f.getMedicine().getId())
                .medicineName(f.getMedicine().getName())
                .characteristic(f.getMedicine().getCharacteristic())
                .imageUrl(f.getMedicine().getImageUrl())
                .prescribed(f.getMedicine().getPrescribed())
                .build();
    }
}