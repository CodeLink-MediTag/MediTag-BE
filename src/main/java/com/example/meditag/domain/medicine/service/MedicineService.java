package com.example.meditag.domain.medicine.service;

import com.example.meditag.domain.alarm.service.AlarmService;
import com.example.meditag.domain.calendar.service.CalendarService;
import com.example.meditag.domain.medicine.dto.request.MedicineCreateRequestDTO;
import com.example.meditag.domain.medicine.dto.response.MedicineCreateResponseDTO;
import com.example.meditag.domain.medicine.entity.Medicine;
import com.example.meditag.domain.medicine.mapper.MedicineMapper;
import com.example.meditag.domain.medicine.repository.MedicineRepository;
import com.example.meditag.domain.member.entity.Member;
import com.example.meditag.domain.member.repository.MemberRepository;
import com.example.meditag.global.aws.S3Service;
import com.example.meditag.global.error.exception.CustomException;
import com.example.meditag.global.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.example.meditag.domain.calendar.entity.Calendar;
import com.example.meditag.domain.calendar.repository.CalendarRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicineService {

    private final MemberRepository memberRepository;
    private final MedicineRepository medicineRepository;
    private final S3Service s3Service;
    private final AlarmService alarmService;
    private final CalendarService calendarService;  // 캘린더 리포지토리 추가

    @Transactional
    public MedicineCreateResponseDTO createMedicine(String username, MedicineCreateRequestDTO requestDto, MultipartFile file) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 처방약인 경우
        if (requestDto.isPrescribed()) {
            if (requestDto.getDosageTimes() == null || requestDto.getDosageTimes().isEmpty()) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
            // 해당 회원이 이미 처방약을 가지고 있는지 확인
            if (medicineRepository.existsByMemberAndPrescribedTrue(member)) {
                throw new CustomException(ErrorCode.ALREADY_HAS_PRESCRIBED_MEDICINE);
            }
        }
        // 일반약인 경우
        else {
            if (requestDto.getAlarmTimes() == null || requestDto.getAlarmTimes().isEmpty()) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
            // frequency와 alarmTimes 개수 비교
            if (requestDto.getFrequency() != requestDto.getAlarmTimes().size()) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }

        // 이미지 업로드 처리 및 URL 획득
        String imageUrl = uploadImageIfPresent(file);

        // Medicine 엔티티 생성
        Medicine medicine = MedicineMapper.toMedicine(requestDto, member, imageUrl);

        Medicine savedMedicine = medicineRepository.save(medicine);

        // 알람 생성 로직 추가
        if (requestDto.isPrescribed()) {
            // 처방약의 경우 dosageTimes 사용 (아침, 점심, 저녁 등)
            if (requestDto.getDosageTimes() != null && !requestDto.getDosageTimes().isEmpty()) {
                alarmService.createAlarmsForPrescribedMedicine(savedMedicine, requestDto.getDosageTimes(), requestDto.getAlarmTimes());
            } else {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        } else {
            // 일반약의 경우 alarmTimes 사용 (구체적인 시간 목록)
            if (requestDto.getAlarmTimes() != null && !requestDto.getAlarmTimes().isEmpty()) {
                alarmService.createAlarmsForNormalMedicine(savedMedicine, requestDto.getAlarmTimes());
            } else {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }

        // 날짜별 Calendar 엔티티 생성
        calendarService.createCalendarForMedicine(savedMedicine, requestDto.getStartDate(), requestDto.getDuration());

        return MedicineMapper.toMedicineCreateResponseDTO(savedMedicine);
    }

    // 이미지 업로드 처리 로직을 별도 메서드로 분리
    private String uploadImageIfPresent(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            String presignedUrl = s3Service.createPresignedUrl("test/" + fileName);

            // S3에 직접 업로드
            URL url = new URL(presignedUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", file.getContentType());

            try (OutputStream os = connection.getOutputStream()) {
                os.write(file.getBytes());
            }

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                // URL에서 query parameter 제거하여 실제 접근 URL만 반환
                return presignedUrl.split("\\?")[0];
            } else {
                throw new RuntimeException("파일 업로드 실패: HTTP 상태 코드 " + connection.getResponseCode());
            }
        } catch (IOException e) {
            throw new RuntimeException("파일 업로드 중 오류 발생", e);
        }
    }

    // Presigned URL만 생성하여 반환하는 메서드 추가
    public String getPresignedUrl(String filename) {
        return s3Service.createPresignedUrl("test/" + filename);
    }
}
