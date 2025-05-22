package com.example.meditag.domain.medicine.service;

import com.example.meditag.domain.alarm.entity.Alarm;
import com.example.meditag.domain.alarm.repository.AlarmRepository;
import com.example.meditag.domain.alarm.service.AlarmSchedulerService;
import com.example.meditag.domain.medicine.dto.request.MedicineCreateRequestDTO;
import com.example.meditag.domain.medicine.dto.request.MedicineUpdateRequestDto;
import com.example.meditag.domain.medicine.dto.response.MedicineResponseDTO;
import com.example.meditag.domain.medicine.dto.response.MedicineGetDateResponseDTO;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.example.meditag.domain.calendar.entity.Calendar;
import com.example.meditag.domain.calendar.repository.CalendarRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicineService {

    private final MemberRepository memberRepository;
    private final MedicineRepository medicineRepository;
    private final CalendarRepository calendarRepository;
    private final AlarmRepository alarmRepository;
    private final S3Service s3Service;
    private final AlarmSchedulerService alarmSchedulerService;

    // 복약 알림 등록 API
    @Transactional
    public MedicineResponseDTO createMedicine(String username, MedicineCreateRequestDTO requestDto, MultipartFile file) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 처방약인 경우
        if (requestDto.getPrescribed()) {
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

        // 복용 날짜(Calendar) 생성
        List<Calendar> calendarList = new ArrayList<>();
        LocalDate startDate = requestDto.getStartDate();
        int duration = requestDto.getDuration();

        for (int i = 0; i < duration; i++) {
            LocalDate medicineDate = startDate.plusDays(i);
            Calendar calendar = Calendar.builder()
                    .date(medicineDate)
                    .medicine(savedMedicine)
                    .build();
            calendarList.add(calendar);
        }
        calendarRepository.saveAll(calendarList); // 캘린더 저장

        // 알람(Alarm) 생성
        List<Alarm> alarmList = new ArrayList<>();

        for (Calendar calendar : calendarList) {
            if (requestDto.getPrescribed()) {
                // 🔥 사용자가 입력한 복용 시간 (`dosageTimes`) 기반으로 생성!
                List<String> dosageTimes = requestDto.getDosageTimes(); // 예: ["점심", "저녁"]
                List<String> alarmTimes = requestDto.getAlarmTimes(); // 예: ["13:00", "20:00"]

                if (dosageTimes.size() != alarmTimes.size()) {
                    throw new CustomException(ErrorCode.INVALID_INPUT_VALUE); // 크기가 맞지 않으면 예외 처리
                }
                for (int i = 0; i < dosageTimes.size(); i++) {
                    LocalTime time = LocalTime.parse(alarmTimes.get(i)); // "08:00" → LocalTime
                    Alarm alarm = Alarm.builder()
                            .calendar(calendar)
                            .dosageTime(dosageTimes.get(i)) // "아침", "점심"
                            .alarmTime(LocalDateTime.of(calendar.getDate(), time))
                            .taking(false)
                            .build();
                    alarmList.add(alarm);
                }
            } else {
                // 일반약이면 사용자가 입력한 알람 시간 사용
                for (String timeString : requestDto.getAlarmTimes()) {
                    LocalTime time = LocalTime.parse(timeString); // 예: "08:00" -> LocalTime
                    Alarm alarm = Alarm.builder()
                            .calendar(calendar)
                            .alarmTime(LocalDateTime.of(calendar.getDate(), time))
                            .taking(false)
                            .build();
                    alarmList.add(alarm);
                }
            }
        }
        alarmRepository.saveAll(alarmList); // 알람 저장

        // 알람 스케줄링 추가
        alarmSchedulerService.scheduleAlarms(savedMedicine, alarmList);

        return MedicineMapper.toMedicineResponseDTO(savedMedicine);
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

    // 특정 날짜 복약 정보 조회 API
    @Transactional
    public MedicineGetDateResponseDTO getMedicinesByDate(String username, String date) {
        // 1. 회원 정보 조회
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 2. 해당 날짜에 복용해야 하는 약 정보 조회
        List<Calendar> calendars = calendarRepository.findByMemberAndDate(member, LocalDate.parse(date));

        if (calendars.isEmpty()) {
            throw new CustomException(ErrorCode.MEDICINE_NOT_FOUND_FOR_DATE);
        }

        // 3. 해당 날짜에 복용해야 하는 약 리스트
        List<MedicineGetDateResponseDTO.MedicineDTO> medicineDTOList = new ArrayList<>();
        for (Calendar calendar : calendars) {
            Medicine medicine = calendar.getMedicine();

            // 4. 알림 시간 조회
            List<Alarm> alarms = alarmRepository.findByMedicineAndDate(medicine, LocalDate.parse(date));

            List<MedicineGetDateResponseDTO.AlarmDTO> alarmDTOs = alarms.stream()
                    .map(alarm -> MedicineGetDateResponseDTO.AlarmDTO.builder()
                            .alarmTime(alarm.getAlarmTime())
                            .isTaking(alarm.isTaking())
                            .build())
                    .collect(Collectors.toList());

            MedicineGetDateResponseDTO.MedicineDTO medicineDTO = MedicineGetDateResponseDTO.MedicineDTO.builder()
                    .medicineId(medicine.getId())
                    .medicineName(medicine.getName())
                    .characteristic(medicine.getCharacteristic())
                    .imageUrl(medicine.getImageUrl())
                    .prescribed(medicine.getPrescribed())
                    .alarms(alarmDTOs)
                    .build();

            medicineDTOList.add(medicineDTO);
        }

        // 5. 최종 DTO 반환
        return MedicineGetDateResponseDTO.builder()
                .date(date)  // 요청된 날짜
                .medicines(medicineDTOList)  // 해당 날짜에 복용해야 할 약 리스트
                .build();
    }

    @Transactional
    public MedicineResponseDTO updateMedicine(String username, Long medicineId, MedicineUpdateRequestDto updateDto, MultipartFile file) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Medicine medicine = medicineRepository.findByIdAndMember(medicineId, member)
                .orElseThrow(() -> new CustomException(ErrorCode.MEDICINE_NOT_FOUND));

        // 이미지 업로드 처리
        String imageUrl = uploadImageIfPresent(file);
        medicine.update(updateDto, imageUrl);
        medicineRepository.save(medicine);

        // 📌 알람 정보를 업데이트할지 판단
        boolean shouldUpdateAlarms = updateDto.getAlarmTimes() != null && !updateDto.getAlarmTimes().isEmpty();

        if (shouldUpdateAlarms) {
            // 알람 스케줄링 취소 및 기존 캘린더/알람 삭제
            alarmSchedulerService.cancelMedicineAlarms(medicineId);
            calendarRepository.deleteAll(calendarRepository.findByMedicine(medicine));

            // 새 캘린더 생성
            LocalDate startDate = updateDto.getStartDate() != null ? updateDto.getStartDate() : medicine.getStartDate();
            int duration = updateDto.getDuration() != null ? updateDto.getDuration() : medicine.getDuration();

            List<Calendar> newCalendars = new ArrayList<>();
            for (int i = 0; i < duration; i++) {
                LocalDate medicineDate = startDate.plusDays(i);
                Calendar calendar = Calendar.builder()
                        .date(medicineDate)
                        .medicine(medicine)
                        .build();
                newCalendars.add(calendar);
            }
            calendarRepository.saveAll(newCalendars);

            // 새 알람 생성
            List<Alarm> newAlarms = new ArrayList<>();
            Boolean prescribed = updateDto.getPrescribed() != null ? updateDto.getPrescribed() : medicine.getPrescribed();

            if (prescribed != null && prescribed) {
                List<String> dosageTimes = updateDto.getDosageTimes();
                List<String> alarmTimes = updateDto.getAlarmTimes();
                for (int i = 0; i < Math.min(dosageTimes.size(), alarmTimes.size()); i++) {
                    LocalTime time = LocalTime.parse(alarmTimes.get(i));
                    for (Calendar calendar : newCalendars) {
                        newAlarms.add(Alarm.builder()
                                .calendar(calendar)
                                .dosageTime(dosageTimes.get(i))
                                .alarmTime(LocalDateTime.of(calendar.getDate(), time))
                                .taking(false)
                                .build());
                    }
                }
            } else {
                for (Calendar calendar : newCalendars) {
                    for (String timeString : updateDto.getAlarmTimes()) {
                        LocalTime time = LocalTime.parse(timeString);
                        newAlarms.add(Alarm.builder()
                                .calendar(calendar)
                                .alarmTime(LocalDateTime.of(calendar.getDate(), time))
                                .taking(false)
                                .build());
                    }
                }
            }

            alarmRepository.saveAll(newAlarms);
            alarmSchedulerService.scheduleAlarms(medicine, newAlarms);
        }

        return MedicineMapper.toMedicineResponseDTO(medicine);
    }

    // 약 알림 삭제 API
    @Transactional
    public void deleteMedicine(String username, Long medicineId) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Medicine medicine = medicineRepository.findByIdAndMember(medicineId, member)
                .orElseThrow(() -> new CustomException(ErrorCode.MEDICINE_NOT_FOUND));

        // 알람 스케줄링 취소
        alarmSchedulerService.cancelMedicineAlarms(medicineId);

        // 캘린더 조회 후 삭제 (Cascade로 알람도 함께 삭제됨)
        List<Calendar> calendars = calendarRepository.findByMedicine(medicine);
        calendarRepository.deleteAll(calendars);

        // 마지막으로 약 삭제
        medicineRepository.delete(medicine);
    }

    // Presigned URL만 생성하여 반환하는 메서드 추가
    public String getPresignedUrl(String filename) {
        return s3Service.createPresignedUrl("test/" + filename);
    }
}
