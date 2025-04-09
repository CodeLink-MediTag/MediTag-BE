package com.example.meditag.domain.chatbot.service.message.register;

import com.example.meditag.domain.alarm.entity.Alarm;
import com.example.meditag.domain.calendar.entity.Calendar;
import com.example.meditag.domain.calendar.repository.CalendarRepository;
import com.example.meditag.domain.chatbot.dto.MedicineRegisterSession;
import com.example.meditag.domain.medicine.entity.Medicine;
import com.example.meditag.domain.medicine.repository.MedicineRepository;
import com.example.meditag.domain.member.entity.Member;
import com.example.meditag.domain.member.repository.MemberRepository;
import com.example.meditag.global.error.exception.CustomException;
import com.example.meditag.global.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicineRegisterService {

    private final MedicineRepository medicineRepository;
    private final MemberRepository memberRepository;
    private final CalendarRepository calendarRepository;
    private final SessionStorage sessionStorage;

    public boolean isApplicable(String message) {
        return message.matches(".*(약|알림).*등록(할게|할래|해줘|해|하고싶어|하고 싶어).*");
    }

    public String execute(String username, String message) {
        if (message.contains("취소") || message.contains("그만")) {
            sessionStorage.clearSession(username);
            log.info("[{}] 사용자가 등록을 취소함", username);
            return "약 등록을 취소했어요. 다시 시작하려면 '약 등록할게'라고 말해주세요.";
        }

        MedicineRegisterSession session = sessionStorage.getOrCreateSession(username);

        if (session.getCurrentStep() == null) {
            session.setCurrentStep(MedicineRegisterSession.Step.NAME);
            log.info("[{}] 세션 시작됨. Step: NAME", username);
            return "약 등록을 시작할게요! 약 이름이 무엇인가요?";
        }

        try {
            log.info("[{}] 현재 Step: {}", username, session.getCurrentStep());
            log.info("[{}] 사용자 입력: {}", username, message);

            return switch (session.getCurrentStep()) {
                case NAME -> {
                    session.setName(message.trim());
                    session.setCurrentStep(MedicineRegisterSession.Step.CHARACTERISTIC);
                    yield "약의 특징은 무엇인가요?";
                }
                case CHARACTERISTIC -> {
                    session.setCharacteristic(message.trim());
                    session.setCurrentStep(MedicineRegisterSession.Step.START_DATE);
                    yield "복용 시작일은 언제인가요? 연도, 월, 일을 정확하게 말해주세요!";
                }
                case START_DATE -> {
                    session.setStartDate(parseNaturalDate(message));
                    session.setCurrentStep(MedicineRegisterSession.Step.DURATION);
                    yield "복용 기간은 며칠인가요?";
                }
                case DURATION -> {
                    int duration = extractDaysFromText(message);
                    session.setDuration(duration);
                    session.setCurrentStep(MedicineRegisterSession.Step.FREQUENCY);
                    yield "하루 복용 횟수는 몇 회인가요?";
                }
                case FREQUENCY -> {
                    int frequency = extractFrequencyFromText(message);
                    session.setFrequency(frequency);
                    session.setCurrentStep(MedicineRegisterSession.Step.PRESCRIBED);
                    yield "이 약은 처방약인가요? (예/아니오)";
                }
                case PRESCRIBED -> {
                    boolean isPrescribed = !message.contains("아니");
                    session.setPrescribed(isPrescribed);

                    if (isPrescribed) {
                        Member member = memberRepository.findByUsername(username)
                                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
                        if (medicineRepository.existsByMemberAndPrescribed(member, true)) {
                            sessionStorage.clearSession(username);
                            yield "이미 등록된 처방약이 존재합니다!";
                        }
                    }

                    session.setCurrentStep(MedicineRegisterSession.Step.ALARM_DOSAGE_TIME);
                    yield isPrescribed
                            ? "처방약이네요! 아침, 점심, 저녁 시간대를 각각 입력해주세요! 시간앞에 오전, 오후를 말해주세요"
                            : "복용할 시간들을 입력해주세요! 시간 앞에 오전, 오후를 말해주세요";
                }
                case ALARM_DOSAGE_TIME -> {
                    if (session.getFrequency() == null) {
                        sessionStorage.clearSession(username);
                        yield "복용 횟수 정보가 누락되었습니다. 약 등록을 다시 시작해주세요.";
                    }

                    List<String> times = extractTimesFromText(message);
                    if (times.size() != session.getFrequency()) {
                        yield "복용 횟수에 맞게 정확히 " + session.getFrequency() + "개의 시간을 입력해주세요!";
                    }

                    session.setDosageTimes(times.toArray(new String[0]));
                    session.setCurrentStep(MedicineRegisterSession.Step.COMPLETE);

                    Member member = memberRepository.findByUsername(username)
                            .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

                    Medicine medicine = Medicine.builder()
                            .name(session.getName())
                            .characteristic(session.getCharacteristic())
                            .startDate(session.getStartDate())
                            .duration(session.getDuration())
                            .frequency(session.getFrequency())
                            .prescribed(session.isPrescribed())
                            .imageUrl(null)
                            .member(member)
                            .build();

                    medicineRepository.save(medicine);

                    for (int day = 0; day < session.getDuration(); day++) {
                        LocalDate currentDay = session.getStartDate().plusDays(day);
                        Calendar calendar = Calendar.builder()
                                .date(currentDay)
                                .medicine(medicine)
                                .alarms(new ArrayList<>())
                                .build();

                        for (String timeStr : times) {
                            String[] parts = timeStr.split(":");
                            LocalTime localTime = LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                            LocalDateTime alarmDateTime = LocalDateTime.of(currentDay, localTime);

                            Alarm alarm = Alarm.builder()
                                    .dosageTime(session.isPrescribed() ? getDosageLabel(localTime) : "일반")
                                    .alarmTime(alarmDateTime)
                                    .taking(false)
                                    .calendar(calendar)
                                    .build();

                            calendar.getAlarms().add(alarm);
                        }

                        calendarRepository.save(calendar);
                    }

                    sessionStorage.clearSession(username);
                    yield "약과 알림이 성공적으로 등록되었습니다!";
                }
                default -> {
                    sessionStorage.clearSession(username);
                    yield "약 등록을 다시 시작하려면 '약 등록할게'라고 말씀해주세요.";
                }
            };
        } catch (Exception e) {
            sessionStorage.clearSession(username);
            return "입력 도중 오류가 발생했어요. 다시 시도해주세요.";
        }
    }

    private LocalDate parseNaturalDate(String message) {
        try {
            if (message.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(message);
            }
            Matcher matcher = Pattern.compile("(?:\\b(\\d{4})[년\\s]*)?(\\d{1,2})월\\s*(\\d{1,2})일").matcher(message);
            if (matcher.find()) {
                int year = matcher.group(1) != null ? Integer.parseInt(matcher.group(1)) : LocalDate.now().getYear();
                int month = Integer.parseInt(matcher.group(2));
                int day = Integer.parseInt(matcher.group(3));
                return LocalDate.of(year, month, day);
            }
        } catch (Exception ignored) {}
        throw new IllegalArgumentException("날짜 형식이 올바르지 않습니다.");
    }

    private int extractDaysFromText(String message) {
        message = message.replaceAll("[\\s]", "");
        if (message.contains("일주일") || message.contains("한주") || message.contains("1주") || message.contains("1주일")) return 7;
        if (message.contains("이주일") || message.contains("2주") || message.contains("이주") || message.contains("2주일")) return 14;
        if (message.contains("삼일") || message.contains("3일") || message.contains("삼 일")) return 3;
        if (message.contains("오일") || message.contains("5일") || message.contains("오 일")) return 5;
        if (message.contains("한달") || message.contains("30일") || message.contains("삼십일")) return 30;
        if (message.contains("십오일") || message.contains("보름") || message.contains("15일")) return 15;
        Matcher m = Pattern.compile("(\\d+)[일일간]").matcher(message);
        if (m.find()) return Integer.parseInt(m.group(1));
        return 1;
    }

    private int extractFrequencyFromText(String message) {
        message = message.replaceAll("[\\s]", "");
        if (message.contains("한번") || message.contains("1회")) return 1;
        if (message.contains("두번") || message.contains("2회")) return 2;
        if (message.contains("세번") || message.contains("3회")) return 3;
        if (message.contains("네번") || message.contains("4회")) return 4;
        Matcher m = Pattern.compile("(\\d+)[번회]").matcher(message);
        if (m.find()) return Integer.parseInt(m.group(1));
        return 1;
    }

    private List<String> extractTimesFromText(String message) {
        List<String> result = new ArrayList<>();
        Matcher matcher = Pattern.compile("(오전|오후)?\\s*(\\d{1,2})시(\\d{1,2})?분?").matcher(message);
        while (matcher.find()) {
            String meridiem = matcher.group(1);
            int hour = Integer.parseInt(matcher.group(2));
            int minute = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
            if ("오후".equals(meridiem) && hour < 12) hour += 12;
            else if ("오전".equals(meridiem) && hour == 12) hour = 0;
            result.add(String.format("%02d:%02d", hour, minute));
        }
        if (result.isEmpty()) {
            String[] raw = message.split(",\\s*");
            for (String time : raw) {
                if (time.matches("\\d{1,2}:\\d{2}")) {
                    result.add(time.trim());
                }
            }
        }
        return result;
    }

    private String getDosageLabel(LocalTime time) {
        if (time.isBefore(LocalTime.NOON)) return "아침";
        else if (time.isBefore(LocalTime.of(18, 0))) return "점심";
        else return "저녁";
    }

    public boolean isInProgress(String username) {
        MedicineRegisterSession session = sessionStorage.getOrCreateSession(username);
        return session.getCurrentStep() != null && session.getCurrentStep() != MedicineRegisterSession.Step.COMPLETE;
    }
}