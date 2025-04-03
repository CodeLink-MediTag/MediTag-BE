package com.example.meditag.domain.chatbot.service;

import com.example.meditag.domain.chatbot.dto.DailyUserStatus;
import com.example.meditag.domain.medicine.service.MedicineService;
import com.example.meditag.domain.member.entity.Member;
import com.example.meditag.domain.member.repository.MemberRepository;
import com.example.meditag.domain.record.entity.Recording;
import com.example.meditag.domain.record.repository.RecordRepository;
import com.example.meditag.global.error.exception.CustomException;
import com.example.meditag.global.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DailyStatusService {

    private final MemberRepository memberRepository;
    private final MedicineService medicineService;
    private final RecordRepository recordingRepository;

    // 기존: 특정 날짜 기준
    public DailyUserStatus getDailyStatus(String username, LocalDate date) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 해당 날짜 약 정보
        var medicineData = medicineService.getMedicinesByDate(username, date.toString());

        // 해당 날짜 녹음 기록
        List<Recording> recordings = recordingRepository.findByMemberAndRecordingTimeBetween(
                member,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        );

        return DailyUserStatus.builder()
                .date(date.toString())
                .medicines(medicineData.getMedicines())
                .recordings(recordings)
                .build();
    }

    // ✅ 추가: 전체 날짜 기준 (약 정보 제외)
    public DailyUserStatus getFullStatus(String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        List<Recording> allRecordings = recordingRepository.findAllByMember(member);

        return DailyUserStatus.builder()
                .date("전체") // 또는 null
                .medicines(null) // 전체 약 정보가 필요하다면 여기에 추가
                .recordings(allRecordings)
                .build();
    }
}
