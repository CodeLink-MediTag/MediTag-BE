package com.example.meditag.domain.member.service;

import com.example.meditag.domain.member.dto.request.GuardianRequestDTO;
import com.example.meditag.domain.member.dto.response.GuardianResponseDTO;
import com.example.meditag.domain.member.entity.Guardian;
import com.example.meditag.domain.member.entity.Member;
import com.example.meditag.domain.member.repository.GuardianRepository;
import com.example.meditag.domain.member.repository.MemberRepository;
import com.example.meditag.global.error.exception.CustomException;
import com.example.meditag.global.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuardianService {

    private final GuardianRepository guardianRepository;
    private final MemberRepository memberRepository;

    /**
     * 보호자 등록
     */
    @Transactional
    public GuardianResponseDTO registerGuardian(String username, GuardianRequestDTO requestDTO) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        
        Guardian guardian = Guardian.builder()
                .member(member)
                .phoneNumber(requestDTO.getPhoneNumber())
                .relationship(requestDTO.getRelationship())
                .build();
        
        Guardian savedGuardian = guardianRepository.save(guardian);
        log.info("[GuardianService] 보호자 등록 완료 - 회원: {}, 보호자 번호: {}", 
                username, requestDTO.getPhoneNumber());
        
        return GuardianResponseDTO.from(savedGuardian);
    }

    /**
     * 회원의 모든 보호자 조회
     */
    @Transactional(readOnly = true)
    public List<GuardianResponseDTO> getGuardians(String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        
        List<Guardian> guardians = guardianRepository.findByMember(member);
        
        return guardians.stream()
                .map(GuardianResponseDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * 보호자 삭제
     */
    @Transactional
    public void deleteGuardian(String username, Long guardianId) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        
        Guardian guardian = guardianRepository.findById(guardianId)
                .orElseThrow(() -> new CustomException(ErrorCode.GUARDIAN_NOT_FOUND));
        
        // 해당 보호자가 요청한 회원의 보호자인지 확인
        if (!guardian.getMember().getId().equals(member.getId())) {
            throw new CustomException(ErrorCode.GUARDIAN_NOT_FOUND);
        }
        
        guardianRepository.delete(guardian);
        log.info("[GuardianService] 보호자 삭제 완료 - 회원: {}, 보호자 ID: {}", 
                username, guardianId);
    }
}
